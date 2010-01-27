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

import hudson.model.AbstractBuild;
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
 * Represents the composite baseline release action
 */
public class ClearcaseReleaseCompositeBaselineAction extends ClearcaseReleaseAction {

    private final AbstractBuild owner;

    private final String customReleasePromotionLevel;

    public ClearcaseReleaseCompositeBaselineAction(AbstractBuild owner, String customReleasePromotionLevel) {
        super(owner.getWorkspace());
        this.owner = owner;
        this.customReleasePromotionLevel = customReleasePromotionLevel;
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

        process();

        doIndex(req, resp);
    }

    public void process() {
        SCM scm = owner.getProject().getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            new TagWorkerThread().start();
        }
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

                //Release the composite baseline
                performClearcaseReleaseCompisteBaseline(listener, owner, customReleasePromotionLevel);

                //Save the the build information
                owner.save();
            }
            catch (Throwable e) {
                listener.getLogger().println("[ERROR] - " + e.getMessage());
            }
            finally {
                //reset the worker thread
                workerThread = null;

                listener.getLogger().println("");
            }
        }
    }


}
