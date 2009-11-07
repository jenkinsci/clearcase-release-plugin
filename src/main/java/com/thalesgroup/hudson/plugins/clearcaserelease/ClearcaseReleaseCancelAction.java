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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.security.Permission;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cancel the badge action and reinitialize the release baseline to INITIAL
 */
public class ClearcaseReleaseCancelAction extends ClearcaseReleaseAction {


    private Run owner;

    private final AbstractProject project;

    private ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction;

    private List<String> promotedBaselines = new ArrayList<String>();

    public ClearcaseReleaseCancelAction(Run owner, AbstractProject project, FilePath workspace, ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction, List<String> promotedBaselines) {
        super(workspace);
        this.owner = owner;
        this.project = project;
        this.releaseBuildBadgeAction = releaseBuildBadgeAction;
        this.promotedBaselines = promotedBaselines;
    }


    @SuppressWarnings("unused")
    public Run getOwner() {
        return owner;
    }

    public String getDisplayName() {
        return "Delete the release baseline";
    }

    protected Permission getPermission() {
        return SCM.TAG;
    }

    protected ACL getACL() {
        return owner.getACL();
    }


    public String getIconFileName() {
        //TODO Check if the composite baseline is already promoted to RELEAED (saved filed or clearcase request)
        if (ClearcaseReleaseBuildWrapper.hasReleasePermission(project)) {
            return "edit-delete.gif";
        }
        // by returning null the link will not be shown.
        return null;
    }


    public String getUrlName() {
        return "deleterelease";
    }

    @SuppressWarnings("unused")
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException, InterruptedException {

        // verify permission
        ClearcaseReleaseBuildWrapper.checkReleasePermission(project);

        SCM scm = project.getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM clearCaseUcmSCM = (ClearCaseUcmSCM) scm;
            new TagWorkerThread().start();
        }

        doIndex(req, resp);
    }


    /**
     * The thread that performs tagging operation asynchronously.
     */
    public final class TagWorkerThread extends TaskThread {


        public TagWorkerThread() {
            super(ClearcaseReleaseCancelAction.this, ListenerAndText.forMemory());
        }

        @Override
        protected void perform(TaskListener listener) {
            try {
                listener.getLogger().println("\nClearcase release cancel preforming");
                Launcher launcher = new Launcher.LocalLauncher(listener);
                HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

                //Remove the badge action
                owner.getActions().remove(releaseBuildBadgeAction);

                //Remove itself the cancel release action
                owner.getActions().remove(ClearcaseReleaseCancelAction.this);

                //Cancel the release baseline
                for (String promotedBaseline : promotedBaselines) {
                    changeLevelBaseline(promotedBaseline, TYPE_BASELINE_STATUS.BUILT, clearToolLauncher, workspaceRoot);
                }

                //Unlock the owner
                owner.keepLog(false);

                //Save the build
                owner.save();

                listener.getLogger().println("");

            } catch (Throwable e) {
                e.printStackTrace(listener.fatalError(e.getMessage()));
            }
        }
    }
}
