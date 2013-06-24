/*
 * The MIT License
 *
 * Copyright (c) 2011, Stefan Wolf
 * Copyright (c) 2011, ryg
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.scm.ChangeLogSet;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Action to calculate all changes for a build
 * It uses ChangesAggregators
 *
 * @author wolfs, ryg
 */
public class CustomHistoryAction implements Action {	
    private AbstractProject<?, ?> project;
    transient List<HistoryAggregator> aggregators;

    CustomHistoryAction(AbstractProject<?, ?> project) {
        this.project = project;
    }
    
    
    
    public String getIconFileName() {
        return "notepad.png";
    }

    public String getDisplayName() {
        return "Custom History";
        //return Messages.CustomHistory_CustomChanges();
    }

    public String getUrlName() {
        return "customhistory";
    }

    /**
     * Returns all changes which contribute to a build.
     *
     * @param build
     * @return
     */
    public Multimap<String, AbstractBuild> getAllChanges(AbstractBuild build) {
        Set<AbstractBuild> builds = getContributingBuilds(build);        
        Multimap<String, AbstractBuild> historymmap = ArrayListMultimap.create();
        for (AbstractBuild changedBuild : builds) {
            File dir = build.getArtifactsDir();
            Scanner scanner = null;
            StringBuilder text = new StringBuilder();
            try {
                String NL = System.getProperty("line.separator");
                scanner = new Scanner(
                        new FileInputStream(dir.getAbsoluteFile() 
                        + "/"+SaveHistory.unifiedHistoryFile));
                while (scanner.hasNextLine()) {
                    text.append(scanner.nextLine()).append(NL);
                }
            } catch (Exception e) {
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }

            historymmap.put(text.toString(), changedBuild);
        }
        return historymmap;
    }

    /**
     * Uses all ChangesAggregators to calculate the contributing builds
     *
     * @return all changes which contribute to the given build
     */
    public Set<AbstractBuild> getContributingBuilds(AbstractBuild build) {
        if (aggregators == null) {
            aggregators = ImmutableList.copyOf(HistoryAggregator.all());
        }
        Set<AbstractBuild> builds = Sets.newHashSet();
        builds.add(build);
        int size = 0;
        // Saturate the build Set
        do {
            size = builds.size();
            Set<AbstractBuild> newBuilds = Sets.newHashSet();
            for (HistoryAggregator aggregator : aggregators) {
                for (AbstractBuild depBuild : builds) {
                    newBuilds.addAll(aggregator.aggregateBuildsWithChanges(depBuild));
                }
            }
            builds.addAll(newBuilds);
        } while (size < builds.size());
        return builds;
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }
}
