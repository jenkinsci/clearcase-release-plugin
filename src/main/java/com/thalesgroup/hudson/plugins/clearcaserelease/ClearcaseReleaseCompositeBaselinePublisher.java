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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import com.thalesgroup.hudson.plugins.clearcaserelease.biz.ClearcaseReleaseActionImpl;


public class ClearcaseReleaseCompositeBaselinePublisher extends ClearcaseReleasePublisher {


    @DataBoundConstructor
    public ClearcaseReleaseCompositeBaselinePublisher(String customReleasePromotionLevel) {
        super(customReleasePromotionLevel);
    }

    @Override
    public CRLatestBaselinePublisherDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        new ClearcaseReleaseActionImpl(build.getWorkspace()).performCompoisteBaselineRelease(listener, build, getCustomReleasePromotionLevel());
                        
        return true;
    }

    @Extension
    public static final CRLatestBaselinePublisherDescriptor DESCRIPTOR = new CRLatestBaselinePublisherDescriptor();


    public static final class CRLatestBaselinePublisherDescriptor extends BuildStepDescriptor<Publisher> {


        public CRLatestBaselinePublisherDescriptor() {
            super(ClearcaseReleaseCompositeBaselinePublisher.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.ClearcaseReleaseCompositeBaselinePublisher_displayName();
        }

        @Override
        public final String getHelpFile() {
            return getPluginRoot() + "helpCRCompositeBaseline.html";
        }

        public String getPluginRoot() {
            return "/plugin/clearcase-release/";
        }


    }
}
