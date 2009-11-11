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

import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite;
import hudson.scm.SCM;
import hudson.security.ACL;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * Represents the composite baseline release action
 */
public class ClearcaseReleaseCompositeBaselineAction extends ClearcaseReleaseAction {

    private AbstractBuild owner;

    public ClearcaseReleaseCompositeBaselineAction(AbstractBuild owner) {
        super(owner.getWorkspace());
        this.owner = owner;
    }

    @SuppressWarnings("unused")
    public AbstractBuild getOwner() {
        return owner;
    }

    public String getDisplayName() {
        return Messages.ReleaseAction_perform_buildCompositeBaseline_name();
    }

    protected ACL getACL() {
        return owner.getACL();
    }


    public String getIconFileName() {
        if (hasReleasePermission(owner.getProject())) {
            return "installer.gif";
        }
        // by returning null the link will not be shown.
        return null;
    }


    public String getUrlName() {
        return "clearcasereleasecompositebaseline";
    }

    @SuppressWarnings("unused")
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException, InterruptedException {

        //The logged user must bae the TAG permission
        getACL().checkPermission(SCM.TAG);

        SCM scm = owner.getProject().getScm();
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
            super(ClearcaseReleaseCompositeBaselineAction.this, ListenerAndText.forMemory());
        }

        @Override
        protected void perform(TaskListener listener) {
            try {
                listener.getLogger().println("\nClearcase release preforming");
                Launcher launcher = new Launcher.LocalLauncher(listener);
                HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(listener, launcher);

                //Get the composite baseline information
                UcmMakeBaselineComposite composite = (UcmMakeBaselineComposite) owner.getProject().getPublishersList().get(hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite.class);
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

                    //Promote to RELEASED the compiste baseline
                    listener.getLogger().println("Promote to RELEASED the composite baseline '" + compositeBaseLine + "'");
                    changeLevelBaseline(compositeBaseLine + "@\\" + pvob, TYPE_BASELINE_STATUS.RELEASED, clearToolLauncher, workspaceRoot);

                    //Add a badge icon
                    String compositeBaseNameDescription = compositeBaseLine + ":RELEASED";
                    ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction = new ClearcaseReleaseBuildBadgeAction(compositeBaseNameDescription);
                    owner.addAction(releaseBuildBadgeAction);

                    //Add a cancel action
                    owner.addAction(new ClearcaseReleaseCancelAction(owner, owner.getProject(), workspaceRoot, releaseBuildBadgeAction, Arrays.asList(new String[]{compositeBaseLine + "@\\" + pvob})));

                    // Keep the build
                    owner.keepLog();

                    //Save the the build information
                    owner.save();

                    //reset the worker thread
                    workerThread = null;

                } else {
                    listener.getLogger().println("\nThe composite baseline '" + compositeBaseLine + "' hasn't the status BUILT.");
                }
                listener.getLogger().println("");

            } catch (Throwable e) {
                e.printStackTrace(listener.fatalError(e.getMessage()));
            }
        }
    }


}
