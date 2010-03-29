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

import com.thalesgroup.hudson.plugins.clearcaserelease.biz.ClearcaseReleaseActionImpl;
import hudson.FilePath;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.security.Permission;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * Represents a clearcase action
 */
public abstract class ClearcaseReleaseAction extends TaskAction {

    protected final FilePath workspaceRoot;


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
     * Release a composite baseline
     *
     * @param listener
     * @param owner
     * @param customReleasePromotionLevel
     * @throws IOException
     * @throws InterruptedException
     */
    public void performClearcaseReleaseCompisteBaseline(TaskListener listener, AbstractBuild owner, String customReleasePromotionLevel) throws IOException, InterruptedException {
        new ClearcaseReleaseActionImpl(workspaceRoot).performCompoisteBaselineRelease(listener, owner, customReleasePromotionLevel);
    }


    /**
     * Cancel the baselines pronotion
     *
     * @param listener
     * @param owner
     * @param releaseBuildBadgeAction
     * @param clearcaseReleaseCancelAction
     * @param promotedBaselines
     * @throws IOException
     * @throws InterruptedException
     */
    public void performCancelRelease(TaskListener listener, Run owner,
                                     ClearcaseReleaseBuildBadgeAction releaseBuildBadgeAction,
                                     ClearcaseReleaseCancelAction clearcaseReleaseCancelAction,
                                     List<String> promotedBaselines) throws IOException, InterruptedException {
        new ClearcaseReleaseActionImpl(workspaceRoot).performCancelRelease(
                listener, owner,
                releaseBuildBadgeAction,
                clearcaseReleaseCancelAction,
                promotedBaselines);
    }


    /**
     * Relase lastest baselines
     *
     * @param listener
     * @param project
     * @param owner
     * @param customReleasePromotionLevel
     * @throws IOException
     * @throws InterruptedException
     */
    public void performLatestBaselineRelease(TaskListener listener,
                                            AbstractProject project,
                                            Run owner,
                                            String customReleasePromotionLevel) throws IOException, InterruptedException {

        new ClearcaseReleaseActionImpl(workspaceRoot).performLatestBaselineRelease(
                listener, project, owner,
                customReleasePromotionLevel);
    }


}
