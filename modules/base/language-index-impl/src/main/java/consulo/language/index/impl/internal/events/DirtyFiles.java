// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.events;

import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Conceptually, it is a [project -> List of dirty files for this project]
 * Additionally, it keeps a List of dirty files for an 'unknown' project
 */
public final class DirtyFiles {
    /**
     * List[ (project, dirtyFilesForProject) ]. It should be &lt;=1 entry per project -- which is not guaranteed by
     * this class itself, but by the fact that {@link #addProject} method is not called &gt;1 per project
     * (without corresponding {@link #removeProject}).
     */
    private final List<Pair<Project, ProjectDirtyFiles>> myDirtyFiles = Lists.newLockFreeCopyOnWriteList();
    private final ProjectDirtyFiles myDirtyFilesWithoutProject = new ProjectDirtyFiles();

    public void addFile(Iterable<? extends Project> projects, int fileId) {
        boolean addedToAtLeastOneProject = false;
        for (Project project : projects) {
            ProjectDirtyFiles projectDirtyFiles = findProjectDirtyFiles(project);
            if (projectDirtyFiles != null) {
                // Technically, we can lose this file id if thread suspends here
                // then the project is closed, queue is persisted, and only then thread resumes.
                // To avoid this, we would need to add synchronized blocks when working with myDirtyFiles which will make file events
                // processing slow
                // So I think it's ok to risk some inconsistency
                projectDirtyFiles.addFile(fileId);
                addedToAtLeastOneProject = true;
            }
        }

        // 'projects' parameter may be not empty in the case when a project is not yet removed from ProjectIndexableFilesFilterHolder
        // we just need to make sure that fileId is written to at least one set
        if (!addedToAtLeastOneProject) {
            myDirtyFilesWithoutProject.addFile(fileId);
        }
    }

    public void clear() {
        myDirtyFilesWithoutProject.clear();
        for (Pair<Project, ProjectDirtyFiles> pair : myDirtyFiles) {
            pair.second.clear();
        }
    }

    public void removeProject(Project project) {
        myDirtyFiles.removeIf(pair -> pair.first == project);
    }

    public void removeFile(int fileId) {
        myDirtyFilesWithoutProject.removeFile(fileId);
        for (Pair<Project, ProjectDirtyFiles> pair : myDirtyFiles) {
            pair.second.removeFile(fileId);
        }
    }

    public ProjectDirtyFiles addProject(Project project) {
        ProjectDirtyFiles files = new ProjectDirtyFiles();
        myDirtyFiles.add(Pair.create(project, files));
        return files;
    }

    public List<Project> getProjects() {
        List<Project> projects = new ArrayList<>(myDirtyFiles.size());
        for (Pair<Project, ProjectDirtyFiles> pair : myDirtyFiles) {
            projects.add(pair.first);
        }
        return projects;
    }

    public List<Project> getProjects(int fileId) {
        if (myDirtyFilesWithoutProject.containsFile(fileId)) {
            return Collections.emptyList();
        }
        List<Project> projects = new SmartList<>();
        for (Pair<Project, ProjectDirtyFiles> pair : myDirtyFiles) {
            if (pair.second.containsFile(fileId)) {
                projects.add(pair.first);
            }
        }
        return projects;
    }

    public @Nullable ProjectDirtyFiles getProjectDirtyFiles(@Nullable Project project) {
        if (project == null) {
            return myDirtyFilesWithoutProject;
        }
        return findProjectDirtyFiles(project);
    }

    private @Nullable ProjectDirtyFiles findProjectDirtyFiles(Project project) {
        for (Pair<Project, ProjectDirtyFiles> pair : myDirtyFiles) {
            if (pair.first == project) {
                return pair.second;
            }
        }
        return null;
    }
}
