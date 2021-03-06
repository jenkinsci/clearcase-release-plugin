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
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;


/**
 * Wraps the build by adding a clearcase release build action
 * and a clearcase release project action
 */
public class ClearcaseReleaseBuildWrapper extends BuildWrapper {

    private String customReleasePromotionLevel;

    @DataBoundConstructor
    public ClearcaseReleaseBuildWrapper(String customReleasePromotionLevel) {
        if (customReleasePromotionLevel != null && customReleasePromotionLevel.trim().length() == 0) {
            this.customReleasePromotionLevel = null;
        } else {
            this.customReleasePromotionLevel = customReleasePromotionLevel;
        }
    }

    @Override
    public Action getProjectAction(AbstractProject job) {
        return new ClearcaseReleaseLatestBaselineAction(job, customReleasePromotionLevel);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        UcmMakeBaselineComposite composite = (UcmMakeBaselineComposite) build.getProject().getPublishersList().get(hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite.class);
        if (composite != null) {
            build.addAction(new ClearcaseReleaseCompositeBaselineAction(build, customReleasePromotionLevel));
        }

        return new Environment(){};
    }


    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(ClearcaseReleaseBuildWrapper.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.Wrapper_DisplayName();
        }

        @Override
        public final String getHelpFile() {
            return getPluginRoot() + "helpCRWrapper.html";
        }

        public String getPluginRoot() {
            return "/plugin/clearcase-release/";
        }
    }


    @SuppressWarnings("unused")
    public String getCustomReleasePromotionLevel() {
        return customReleasePromotionLevel;
    }
}
