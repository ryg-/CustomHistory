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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.File;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author ryg
 */
public class SaveHistory extends Notifier {
	public static final String  unifiedHistoryFile = "customHistoryUnified.log";
	public final String fname;	
    public final boolean exitOnFail; 
    
	@DataBoundConstructor
    public SaveHistory(String fname, boolean exitOnFail) {
		//this.getDescriptor().load();
    	this.fname = fname;
    	this.exitOnFail = exitOnFail;
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
        String dir;
        listener.getLogger().println(
        		"Perfrom custom history collecting to file:"+fname);        
        try {
        	dir = build.getArtifactsDir().getCanonicalPath();
            File unifiedFile  = new File (dir,unifiedHistoryFile);
        	File localFile  = new File (dir,fname);
            listener.getLogger().println("**ArtifactsFile= " 
            						+ localFile.getAbsolutePath());        	
            FilePath ws = build.getWorkspace();
            if (ws == null && exitOnFail == true) { // #3330: slave down?
                return false;
            }
            if (ws == null){
            	return true;
            }
            listener.getLogger().println("**WS = " + ws.getBaseName());

            String historyfile = fname;
            int f = ws.copyRecursiveTo(historyfile, null, new FilePath(build.getArtifactsDir()));
            if (f == 0) {
                if (build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                    // If the build failed, don't complain that there was no matching artifact.
                    // The build probably didn't even get to the point where it produces artifacts. 
                    listener.getLogger().println("No custom history collected!");
                    if(exitOnFail == true){
                    	return false;
                    }
                } else {
                    listener.getLogger().println(f + " custom history collected!");
                }
                return true;
            }
            if(unifiedFile.exists()){
            	listener.getLogger().println(unifiedFile.getAbsolutePath() 
            			+ " alredy exists, but this file name is reserved!");
            	return false;
            }
            boolean renameResult = localFile.renameTo(unifiedFile);
            if(!renameResult){
            	listener.getLogger().println(
            			"Cannow rename to file:"+
            					unifiedFile.getAbsolutePath() );
            	return false;
            }
        } catch (Exception e) {
            listener.getLogger().println(
            		"Caught exception while collecting custom history" + e);
            if(exitOnFail == true){
            	return false;
            }
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
