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
import hudson.model.AbstractProject;
import hudson.model.TaskAction;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.PluginImpl;
import hudson.scm.SCM;
import hudson.security.Permission;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Represents a clearcase action
 */
public abstract class ClearcaseReleaseAction extends TaskAction {

    protected final FilePath workspaceRoot;

    /**
     * The promotion level possibilities
     */
    protected static enum BASELINE_PROMOTION_LEVEL {

        RELEASED("RELEASED"),
        BUILT("BUILT");

        private String level;

        private BASELINE_PROMOTION_LEVEL(String level) {
            this.level = level;
        }

        public String getLevel(){
            return this.level;
        }
    }


    /**
     * Defaults to {@link SCM#TAG}.
     */
    protected Permission getPermission() {
        return SCM.TAG;
    }


    protected ClearcaseReleaseAction(FilePath workapace) {
        this.workspaceRoot = workapace;
    }

    /**
     * Retieve the Clearcase launcher
     *
     * @param listener the Hudson listener
     * @param launcher the Hudson launcher
     * @return the Clearcase launcher
     */
    protected HudsonClearToolLauncher getHudsonClearToolLauncher(TaskListener listener, Launcher launcher) {
        HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
                PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), "clearcase-release", listener, workspaceRoot, launcher);

        return clearToolLauncher;
    }

    /**
     * Release actions is given by the SCM actions
     *
     * @param job the current project
     * @return true if the BUILD permission is set
     */
    protected boolean hasReleasePermission(AbstractProject job) {
        return job.hasPermission(getPermission());
    }

    /**
     * Select the view to display
     *
     * @param req the request object
     * @param rsp the response page
     * @throws IOException
     * @throws ServletException
     */
    protected void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        req.getView(this, chooseAction()).forward(req, rsp);
    }

    /**
     * Choose the page to display during the release processing
     *
     * @return the displayed page
     */
    protected synchronized String chooseAction() {
        if (workerThread != null)
            return "inProgress.jelly";
        return "index.jelly";
    }

    /**
     * Get the status of a given UCM baseline
     *
     * @param baseLine          the UCM baseline
     * @param pvob              the UCM P_VOB
     * @param clearToolLauncher : the clearcase object for launching commands
     * @param filePath          the location where to launch the clearcase command
     * @return the baseline status : INITIAL, BUILT, REJECTED, RELEASED or OTHERS
     * @throws IOException
     * @throws InterruptedException
     */
    //cleartool lsbl -fmt "%[plevel]p" P_TracMngt_Rqtf_CoreModel_V3.0.1@\P_ORC
    //RELEASED
    protected String getStatusBaseLine(
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
    protected void changeLevelBaseline(String baselineNameWithPVOB,
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
}