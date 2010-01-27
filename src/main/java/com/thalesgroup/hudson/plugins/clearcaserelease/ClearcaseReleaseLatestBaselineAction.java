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

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.scm.SCM;
import hudson.security.ACL;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;


/**
 * Represents the latest baselines release action
 */
public class ClearcaseReleaseLatestBaselineAction extends ClearcaseReleaseAction {

    private final AbstractProject project;

    private final String customReleasePromotionLevel;

    public ClearcaseReleaseLatestBaselineAction(AbstractProject project, String customReleasePromotionLevel) {
        super(project.getWorkspace());
        this.project = project;
        this.customReleasePromotionLevel = customReleasePromotionLevel;
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


    @SuppressWarnings("unused")
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException, InterruptedException {

        //The logged user must bae the TAG permission
        getACL().checkPermission(SCM.TAG);

        process();

        doIndex(req, resp);
    }

    public void process() {
        SCM scm = project.getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            new TagWorkerThread().start();
        }
    }

    /**
     * The thread that performs tagging operation asynchronously.
     */
    public final class TagWorkerThread extends TaskThread {


        public TagWorkerThread() {
            super(ClearcaseReleaseLatestBaselineAction.this, ListenerAndText.forMemory());
        }

        @Override
        protected void perform(TaskListener listener) {
            try {

                Run owner = getOwner();

                //Process release latest baselines
                peformLatestBaselineRelease(listener, project, owner, customReleasePromotionLevel);

                //Save the the build information
                owner.save();

            } catch (Throwable e) {
                listener.getLogger().println("[ERROR] - " + e.getMessage());
            }
            finally {
                //reset the worker thread
                workerThread = null;
            }


        }
    }

}
