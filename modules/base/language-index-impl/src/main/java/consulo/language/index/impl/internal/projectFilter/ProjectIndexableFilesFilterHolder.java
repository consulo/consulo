// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.projectFilter;

import consulo.application.util.registry.Registry;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public sealed interface ProjectIndexableFilesFilterHolder permits IncrementalProjectIndexableFilesFilterHolder {
    static boolean usePersistentFilesFilter() {
        return Registry.is("persistent.index.files.filter.enabled", true);
    }

    @Nullable ProjectIndexableFilesFilter getProjectIndexableFiles(Project project);

    /**
     * @return list of projects the fileId is contained in (or was added to) the apt project filter
     */
    List<Project> ensureFileIdPresent(int fileId, Supplier<Set<Project>> projects);

    void addFileId(int fileId, Project project);

    void resetFileIds(Project project);

    void removeFile(int fileId);

    @Nullable Project findProjectForFile(int fileId);

    List<Project> findProjectsForFile(int fileId);

    void onProjectClosing(Project project, long vfsCreationTimestamp);

    void onProjectOpened(Project project, long vfsCreationTimestamp);

    /**
     * This is a temp method
     */
    boolean wasDataLoadedFromDisk(Project project);
}
