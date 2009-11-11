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

package com.thalesgroup.hudson.plugins.clearcaserelease;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Represents the latest baselines release action
 */
public class ClearcaseReleaseLatestBaselineAction extends ClearcaseReleaseAction {

    private final AbstractProject project;

    public ClearcaseReleaseLatestBaselineAction(AbstractProject project) {

        super(project.getWorkspace());
        this.project = project;
    }

    @SuppressWarnings("unused")
    public Run getOwner() {
        return project.getLastSuccessfulBuild();
    }

    public String getUrlName() {
        return "clearcasereleaselatestbaseline";
    }


    /**
     * Gets the icon if there is at least one success build (or unstable)
     * and the current user has the right authorizations
     *
     * @return the icon to display
     */
    public String getIconFileName() {
        Run lastBuild = project.getLastSuccessfulBuild();
        if (lastBuild != null && hasReleasePermission(project)) {
            return "installer.gif";
        }
        // by returning null the link will not be shown.
        return null;
    }


    public String getDisplayName() {
        return Messages.ReleaseAction_perform_latestBaselines_name();
    }

    protected ACL getACL() {
        return getOwner().getACL();
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


    @SuppressWarnings("unused")
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException, InterruptedException {

        //The logged user must bae the TAG permission
        getACL().checkPermission(SCM.TAG);

        SCM scm = project.getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM clearCaseUcmSCM = (ClearCaseUcmSCM) scm;
            new TagWorkerThread(clearCaseUcmSCM).start();
        }

        doIndex(req, resp);
    }

    /**
     * The thread that performs tagging operation asynchronously.
     */
    public final class TagWorkerThread extends TaskThread {


        private final ClearCaseUcmSCM clearCaseUcmSCM;

        public TagWorkerThread(ClearCaseUcmSCM clearCaseUcmSCM) {
            super(ClearcaseReleaseLatestBaselineAction.this, ListenerAndText.forMemory());
            this.clearCaseUcmSCM = clearCaseUcmSCM;
        }

        @Override
        protected void perform(TaskListener listener) {
            try {

                Run owner = getOwner();

                listener.getLogger().println("Performing the release of the latest baselines");

                String streamWithPVOB = clearCaseUcmSCM.getStream();
                String pvob = streamWithPVOB;
                if (pvob.contains("@" + File.separator)) {
                    pvob = pvob.substring(pvob.indexOf("@" + File.separator) + 2, pvob.length());
                }

                Launcher launcher = new Launcher.LocalLauncher(listener);
                HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

                //Get all the latest baselines
                List<String> latestBaselines = getLatestBaselines(streamWithPVOB, clearToolLauncher, workspaceRoot);

                //Get the read/write components
                List<String> modComps = getModComponentsFromStream(streamWithPVOB, clearToolLauncher, workspaceRoot);

                //Filtering
                List<String> keepBaselines = new ArrayList<String>();
                for (String latestBaseline : latestBaselines) {

                    //Retrieve the component of the baseline
                    String comp = getComponentFromBaseline(latestBaseline, clearToolLauncher, workspaceRoot);

                    //Keep on the a modifiable component
                    if (modComps.contains(comp)) {
                        keepBaselines.add(latestBaseline);
                    }
                }

                if (keepBaselines.size() == 0) {
                    listener.getLogger().println("There is not baseline to promote to RELEASE");
                    //reset the worker thread
                    workerThread = null;
                    return;
                }

                //Promotion to RELEASED all the latest baseline on modifiable component
                StringBuffer latestBls = new StringBuffer();
                for (String latestBaselineWithPVOB : keepBaselines) {
                    changeLevelBaseline(latestBaselineWithPVOB, TYPE_BASELINE_STATUS.RELEASED, clearToolLauncher, workspaceRoot);
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

                //Save the the build information
                owner.save();

                //reset the worker thread
                workerThread = null;

            } catch (Throwable e) {
                e.printStackTrace(listener.fatalError(e.getMessage()));
            }
        }
    }

}
