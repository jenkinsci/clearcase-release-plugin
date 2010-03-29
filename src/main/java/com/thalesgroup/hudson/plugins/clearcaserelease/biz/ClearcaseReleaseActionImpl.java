/*******************************************************************************
 * Copyright (c) 2009 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot                                                   *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/

package com.thalesgroup.hudson.plugins.clearcaserelease.biz;

import com.thalesgroup.hudson.plugins.clearcaserelease.ClearcaseReleaseBuildBadgeAction;
import com.thalesgroup.hudson.plugins.clearcaserelease.ClearcaseReleaseCancelAction;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.PluginImpl;
import hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ClearcaseReleaseActionImpl {

    protected final FilePath workspaceRoot;


    public ClearcaseReleaseActionImpl(FilePath workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    /**
     * The promotion level possibilities
     */
    private static enum BASELINE_PROMOTION_LEVEL {

        RELEASED("RELEASED"),
        BUILT("BUILT");

        private String level;

        private BASELINE_PROMOTION_LEVEL(String level) {
            this.level = level;
        }

        public String getLevel() {
            return this.level;
        }
    }

    /**
     * Retieve the Clearcase launcher
     *
     * @param listener the Hudson listener
     * @param launcher the Hudson launcher
     * @return the Clearcase launcher
     */
    private HudsonClearToolLauncher getHudsonClearToolLauncher(TaskListener listener, Launcher launcher) {
        HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
                PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), "clearcase-release", listener, workspaceRoot, launcher);

        return clearToolLauncher;
    }

    /**
     * Get the status of a given UCM baseline
     *
     * @param baseLine          the UCM baseline
     * @param pvob              the UCM P_VOB
     * @param clearToolLauncher : the clearcase object for launching commands
     * @param filePath          the location where to launch the clearcase command
     * @return the baseline status : INITIAL, BUILT, REJECTED, RELEASED or OTHERS
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    //cleartool lsbl -fmt "%[plevel]p" P_TracMngt_Rqtf_CoreModel_V3.0.1@\P_ORC
    //RELEASED
    private String getStatusBaseLine(
            String baseLine,
            String pvob,
            HudsonClearToolLauncher clearToolLauncher,
            FilePath filePath)
            throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsbl");
        cmd.add("-fmt");
        cmd.add("\"%[plevel]p\"");
        cmd.add(baseLine + "@" + File.separator + pvob);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();

        String componentName = baos.toString();

        return componentName;
    }

    /**
     * Change the level of an UCM  baseline (composite or not)
     *
     * @param baselineNameWithPVOB the given baseline with the P_VOB
     * @param status               the new baseline status
     * @param clearToolLauncher    the clearcase object launcher
     * @param filePath             the location where to launch the clearcase coommand
     * @throws InterruptedException
     * @throws IOException
     */
    //cleartool chbl -level RELEASED C_hudson-test-2_2009-10-29_18-36-07@\P_ORC
    private void changeLevelBaseline(String baselineNameWithPVOB,
                                     String status,
                                     HudsonClearToolLauncher clearToolLauncher,
                                     FilePath filePath)
            throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("chbl");
        cmd.add("-level");
        cmd.add(status);
        cmd.add(baselineNameWithPVOB);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);
    }

    /**
     * Get the read/write components for a given stream
     *
     * @param streamPVOB        the stream name with the P_VOB
     * @param clearToolLauncher the clearcase launcher object
     * @param filePath          the location where to launch the clearcase command
     * @return the component name
     * @throws IOException
     * @throws InterruptedException
     */
    /*
    cleartool lsstream -fmt "%[mod_comps]CXp" P_LinkMgt_V4.0.0_int@\P_ORC
    -->component:TracMgt_Rqtf_QueryGen@\P_ORC, component:PapeeteReqtifyConnector@\P_ORC, component:LinkMgt_Reqtify@\P_ORC
    */
    private List<String> getModComponentsFromStream(
            String streamPVOB,
            HudsonClearToolLauncher clearToolLauncher,
            FilePath filePath)
            throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsstream");
        cmd.add("-fmt");
        cmd.add("\"%[mod_comps]p\"");
        cmd.add(streamPVOB);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();

        String reusltClt = baos.toString();
        return Arrays.asList(reusltClt.split(" "));
    }


    /**
     * Get the latest baselines for a given stream
     *
     * @param streanWithPVOB    an UCM stream concatened with the PVOB
     * @param clearToolLauncher the clercase object launcher
     * @param filePath          the location  where to launch the clearcase command
     * @return the list of baseline name
     * @throws IOException
     * @throws InterruptedException
     */
    /*
    cleartool lsstream -fmt "%[latest_bls]p" P_LinkMgt_V4.0.0_int@\P_ORC
    -->
    baseline:P_TracMngt_Rqtf_CoreModel_V3.0.1@\P_ORC,
    baseline:LinkManager-4.3.0-2009-10-29_11-03-52.9990@\P_ORC,
    baseline:LinkManager-4.3.0-2009-10-29_11-03-52.2547@\P_ORC,
    baseline:LinkManager-4.3.0-2009-10-29_11-03-52@\P_ORC
    */
    private List<String> getLatestBaselines(
            String streanWithPVOB,
            HudsonClearToolLauncher clearToolLauncher,
            FilePath filePath)
            throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsstream");
        cmd.add("-fmt");
        cmd.add("\"%[latest_bls]CXp\"");
        cmd.add(streanWithPVOB);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        String resultClt = baos.toString();

        //Remove the 'baseline:' prefix
        resultClt = resultClt.replace("baseline:", "");

        //Slit the result. Within the result, each result has a space.
        return Arrays.asList(resultClt.split(", "));
    }


    /**
     * Get the component for a given baseline
     *
     * @param baseLineWithPVOB  the given baseline name concatened with the PVOB
     * @param clearToolLauncher the clearcase launcher object
     * @param filePath          the location where to launch the clearcase command
     * @return the component object
     * @throws IOException
     * @throws InterruptedException
     */
    //cleartool lsbl -fmt "%[component]p" P_TracMngt_Rqtf_CoreModel_V3.0.1@\P_ORC
    //TracMgt_Rqtf_CoreModel
    private String getComponentFromBaseline(
            String baseLineWithPVOB,
            HudsonClearToolLauncher clearToolLauncher,
            FilePath filePath)
            throws IOException, InterruptedException {


        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsbl");
        cmd.add("-fmt");
        cmd.add("\"%[component]p\"");
        cmd.add(baseLineWithPVOB);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();

        String componentName = baos.toString();
        return componentName;
    }


    public void performCompoisteBaselineRelease(TaskListener listener, AbstractBuild owner, String customReleasePromotionLevel) throws IOException, InterruptedException {
        listener.getLogger().println("\nClearcase release preforming");
        Launcher launcher = new Launcher.LocalLauncher(listener);
        HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

        //Get the composite baseline information
        UcmMakeBaselineComposite composite = (UcmMakeBaselineComposite) owner.getProject().getPublishersList().get(hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite.class);
        if (composite == null) {
            listener.getLogger().println("[ERROR] - No composite baseline has been configured for the job.");
        } else {
            String compositeBaseLine = composite.getCompositeNamePattern();
            compositeBaseLine = Util.replaceMacro(compositeBaseLine, owner.getEnvironment(listener));

            //Get the PVOB from the composite stream
            String compositeStreamSelector = composite.getCompositeStreamSelector();
            String pvob = compositeStreamSelector;
            if (compositeStreamSelector.contains("@" + File.separator)) {
                pvob = compositeStreamSelector.substring(compositeStreamSelector.indexOf("@" + File.separator) + 2, compositeStreamSelector.length());
            }

            //Check the status
            listener.getLogger().println("Check the status of the composite baseline '" + compositeBaseLine + "'");
            String compositeBaselineStatus = getStatusBaseLine(compositeBaseLine, pvob, clearToolLauncher, workspaceRoot);

            if ("BUILT".equals(compositeBaselineStatus)) {

                //Promote to the release promotion level the compiste baseline
                String status = (customReleasePromotionLevel == null) ? BASELINE_PROMOTION_LEVEL.RELEASED.getLevel() : customReleasePromotionLevel;
                listener.getLogger().println("Promote to the release promotion level the composite baseline '" + compositeBaseLine + "' with the level '" + status + '"');
                changeLevelBaseline(compositeBaseLine + "@\\" + pvob, status, clearToolLauncher, workspaceRoot);
                listener.getLogger().println("");

                //Add a badge icon
                String compositeBaseNameDescription = compositeBaseLine + ":RELEASED";
                ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction = new ClearcaseReleaseBuildBadgeAction(compositeBaseNameDescription);
                owner.addAction(releaseBuildBadgeAction);

                //Add a cancel action
                owner.addAction(new ClearcaseReleaseCancelAction(owner, owner.getProject(), workspaceRoot, releaseBuildBadgeAction, Arrays.asList(new String[]{compositeBaseLine + "@\\" + pvob})));

                // Keep the build
                owner.keepLog();

            } else {
                listener.getLogger().println("\nThe composite baseline '" + compositeBaseLine + "' hasn't the status BUILT.");
            }
        }
    }


    public void performCancelRelease(TaskListener listener,
                                     Run owner,
                                     ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction,
                                     ClearcaseReleaseCancelAction clearcaseReleaseCancelAction,
                                     List<String> promotedBaselines) throws IOException, InterruptedException {

        listener.getLogger().println("\nClearcase release cancel preforming");
        Launcher launcher = new Launcher.LocalLauncher(listener);
        HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

        //Cancel the release baseline
        for (String promotedBaseline : promotedBaselines) {
            changeLevelBaseline(promotedBaseline, BASELINE_PROMOTION_LEVEL.BUILT.getLevel(), clearToolLauncher, workspaceRoot);
            listener.getLogger().println("");
        }

        //Remove the badge action
        owner.getActions().remove(releaseBuildBadgeAction);

        //Remove itself the cancel release action
        owner.getActions().remove(clearcaseReleaseCancelAction);

        //Unlock the owner
        owner.keepLog(false);
    }

    public void performLatestBaselineRelease(TaskListener listener,
                                            AbstractProject project,
                                            Run owner,
                                            String customReleasePromotionLevel) throws IOException, InterruptedException {
        listener.getLogger().println("\nClearcase release preforming");
        Launcher launcher = new Launcher.LocalLauncher(listener);

        ClearCaseUcmSCM clearCaseUcmSCM = (ClearCaseUcmSCM) project.getScm();

        HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

        listener.getLogger().println("Performing the release of the latest baselines");

        String streamWithPVOB = clearCaseUcmSCM.getStream();
        String pvob = streamWithPVOB;
        if (pvob.contains("@" + File.separator)) {
            pvob = pvob.substring(pvob.indexOf("@" + File.separator) + 2, pvob.length());
        }


        //Get all the latest baselines
        List<String> latestBaselines = getLatestBaselines(streamWithPVOB, clearToolLauncher, workspaceRoot);
        listener.getLogger().println("");

        //Get the read/write components
        List<String> modComps = getModComponentsFromStream(streamWithPVOB, clearToolLauncher, workspaceRoot);
        listener.getLogger().println("");

        //Filtering
        List<String> keepBaselines = new ArrayList<String>();
        for (String latestBaseline : latestBaselines) {

            //Retrieve the component of the baseline
            String comp = getComponentFromBaseline(latestBaseline, clearToolLauncher, workspaceRoot);
            listener.getLogger().println("");

            //Keep on the a modifiable component
            if (modComps.contains(comp)) {
                keepBaselines.add(latestBaseline);
            }
        }

        if (keepBaselines.size() == 0) {
            listener.getLogger().println("There is not baseline to promote to RELEASE");
            return;
        }

        //Promotion to RELEASED all the latest baseline on modifiable component
        StringBuffer latestBls = new StringBuffer();
        for (String latestBaselineWithPVOB : keepBaselines) {
            String status = (customReleasePromotionLevel == null) ? BASELINE_PROMOTION_LEVEL.RELEASED.getLevel() : customReleasePromotionLevel;
            changeLevelBaseline(latestBaselineWithPVOB, status, clearToolLauncher, workspaceRoot);
            listener.getLogger().println("");
            latestBls.append(";");
            latestBls.append(latestBaselineWithPVOB);
        }
        if (latestBls.length() != 0) {
            latestBls.delete(0, 1);
        }

        //Add a badge icon
        String latestBaselinesReleaseDescription = "The latest baseline has been released";
        ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction = new ClearcaseReleaseBuildBadgeAction(latestBaselinesReleaseDescription);
        owner.addAction(releaseBuildBadgeAction);

        ArrayList<ParameterValue> parameters = new ArrayList<ParameterValue>();
        parameters.add(new StringParameterValue("LATEST_BASELINE", latestBls.toString()));
        owner.addAction(new ParametersAction(parameters));


        //Add a cancel action
        owner.addAction(new ClearcaseReleaseCancelAction(owner, project, workspaceRoot, releaseBuildBadgeAction, keepBaselines));

        // Keep the build
        owner.keepLog();

    }
}
