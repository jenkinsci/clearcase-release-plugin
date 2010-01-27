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

import hudson.cli.declarative.CLIMethod;
import hudson.cli.declarative.CLIResolver;
import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Run;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;


public class ClearcaseReleaseCLI {

    private AbstractItem job;

    private int buildnumber;

    public ClearcaseReleaseCLI(AbstractItem job, int buildnumber) {
        this.job = job;
        this.buildnumber = buildnumber;
    }

    @CLIResolver
    public static ClearcaseReleaseCLI resolveForCLI(
            @Argument(required = true, metaVar = "NAME", usage = "Job name") String name,
            @Argument(index = 1, required = false, metaVar = "BUILD#", usage = "Build number") String id)
            throws CmdLineException {

        AbstractItem item = Hudson.getInstance().getItemByFullName(name, AbstractItem.class);
        if (item == null)
            throw new CmdLineException(null, "A job parameter is required");

        int buildnumber = 0;
        if (id != null) {
            try {
                buildnumber = Integer.parseInt(id);
            }
            catch (NumberFormatException nfe) {
                throw new CmdLineException(null, "A job build number (number) is required :" + id);
            }
        }

        return new ClearcaseReleaseCLI(item, buildnumber);

    }

    @CLIMethod(name = "clearcaseCancelRelease")
    @SuppressWarnings("unused")
    public synchronized void clearcaseCancelRelease() throws IOException, InterruptedException {
        Run run = (Run) (((Job) this.job).getBuildByNumber(buildnumber));
        ClearcaseReleaseCancelAction clearcaseReleaseCancelAction = run.getAction(ClearcaseReleaseCancelAction.class);
        clearcaseReleaseCancelAction.process();
    }

    @CLIMethod(name = "clearcasePromoteCompositeBaseline")
    @SuppressWarnings("unused")
    public synchronized void clearcasePromoteCompositeBaseline() throws IOException, InterruptedException {
        Run run = (Run) (((Job) this.job).getBuildByNumber(buildnumber));
        ClearcaseReleaseCompositeBaselineAction clearcaseReleaseCompositeBaselineAction = run.getAction(ClearcaseReleaseCompositeBaselineAction.class);
        clearcaseReleaseCompositeBaselineAction.process();
    }

    @CLIMethod(name = "clearcasePromoteLatestBaselines")
    @SuppressWarnings("unused")
    public synchronized void clearcasePromoteLatestBaselines() throws IOException, InterruptedException {
        ClearcaseReleaseLatestBaselineAction clearcaseReleaseLatestBaselineAction = job.getAction(ClearcaseReleaseLatestBaselineAction.class);
        clearcaseReleaseLatestBaselineAction.process();
    }


}


