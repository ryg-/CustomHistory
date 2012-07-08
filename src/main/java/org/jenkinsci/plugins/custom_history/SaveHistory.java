/*
 * The MIT License
 *
 * Copyright 2012 ryg.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.custom_history;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.File;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author ryg
 */
public class SaveHistory extends Notifier {

    @DataBoundConstructor
    public SaveHistory() {
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        File dir = build.getArtifactsDir();
        listener.getLogger().println("**ArtifactsDir= " + dir.getAbsolutePath());
        try {
            FilePath ws = build.getWorkspace();
            if (ws == null) { // #3330: slave down?
                return true;
            }
            listener.getLogger().println("**WS = " + ws.getBaseName());

            String historyfile = "custom_history.txt";
            int f = ws.copyRecursiveTo(historyfile, null, new FilePath(dir));
            if (f == 0) {
                if (build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                    // If the build failed, don't complain that there was no matching artifact.
                    // The build probably didn't even get to the point where it produces artifacts. 
                    listener.getLogger().println("0 custom history collected!");
                } else {
                    listener.getLogger().println(f + " custom history collected!");
                }
                return true;
            }

        } catch (Exception e) {
            listener.getLogger().println("Caught exception" + e);
            return true;
        }

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(SaveHistory.class);
        }

        @Override
        public String getDisplayName() {
            return "Save custom history";
            //new Localizable(ResourceBundleHolder.get(CopyToMasterNotifier.class), "DisplayName").toString();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> item) {
            return true;
        }
    }
}
