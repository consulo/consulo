// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.index.impl.internal.projectFilter;

import consulo.language.index.impl.internal.UnindexedFilesScannerStartup;
import consulo.language.index.impl.internal.UnindexedFilesUpdater;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public final class IncrementalProjectIndexableFilesFilterHolder implements ProjectIndexableFilesFilterHolder {
    private static final Logger LOG = Logger.getInstance(IncrementalProjectIndexableFilesFilterHolder.class);

    private final ConcurrentMap<Project, ProjectIndexableFilesFilter> myProjectFilters = new ConcurrentHashMap<>();

    @Override
    public void onProjectClosing(Project project, long vfsCreationTimestamp) {
        ProjectIndexableFilesFilter filter = myProjectFilters.remove(project);
        if (filter != null) {
            filter.onProjectClosing(project, vfsCreationTimestamp);
        }
    }

    @Override
    public void onProjectOpened(Project project, long vfsCreationTimestamp) {
        ProjectIndexableFilesFilterFactory factory = chooseFactory(project.getName());
        myProjectFilters.put(project, factory.create(project, vfsCreationTimestamp));
    }

    private static ProjectIndexableFilesFilterFactory chooseFactory(String projectName) {
        ProjectIndexableFilesFilterFactory factory = ProjectIndexableFilesFilterHolder.usePersistentFilesFilter()
            ? new PersistentProjectIndexableFilesFilterFactory()
            : new IncrementalProjectIndexableFilesFilterFactory();

        LOG.info(factory.getClass().getSimpleName() + " is chosen as indexable files filter factory for project: " + projectName);
        return factory;
    }

    @Override
    public boolean wasDataLoadedFromDisk(Project project) {
        ProjectIndexableFilesFilter filter = getFilter(project);
        return filter != null && filter.wasDataLoadedFromDisk();
    }

    @Override
    public @Nullable ProjectIndexableFilesFilter getProjectIndexableFiles(Project project) {
        if (!UnindexedFilesScannerStartup.isFirstProjectScanningPerformed(project)
            || UnindexedFilesUpdater.isScanningInProgress(project)) {
            return null;
        }
        return getFilter(project);
    }

    @Override
    public void resetFileIds(Project project) {
        assert UnindexedFilesUpdater.isScanningInProgress(project);

        ProjectIndexableFilesFilter filter = getFilter(project);
        if (filter != null) {
            filter.resetFileIds();
        }
    }

    private @Nullable ProjectIndexableFilesFilter getFilter(Project project) {
        return myProjectFilters.get(project);
    }

    @Override
    public List<Project> ensureFileIdPresent(int fileId, Supplier<Set<Project>> projects) {
        Supplier<Set<Project>> matchedProjects = lazy(projects);
        List<Project> result = new SmartList<>();
        for (Map.Entry<Project, ProjectIndexableFilesFilter> entry : myProjectFilters.entrySet()) {
            Project project = entry.getKey();
            boolean fileIsInProject = entry.getValue().ensureFileIdPresent(fileId, () -> matchedProjects.get().contains(project));
            if (fileIsInProject) {
                result.add(project);
            }
        }
        return result;
    }

    @Override
    public void addFileId(int fileId, Project project) {
        ProjectIndexableFilesFilter filter = getFilter(project);
        if (filter != null) {
            filter.ensureFileIdPresent(fileId, () -> true);
        }
    }

    @Override
    public void removeFile(int fileId) {
        for (ProjectIndexableFilesFilter filter : myProjectFilters.values()) {
            filter.removeFileId(fileId);
        }
    }

    @Override
    public @Nullable Project findProjectForFile(int fileId) {
        for (Map.Entry<Project, ProjectIndexableFilesFilter> entry : myProjectFilters.entrySet()) {
            if (entry.getValue().containsFileId(fileId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public List<Project> findProjectsForFile(int fileId) {
        List<Project> projects = new SmartList<>();
        for (Map.Entry<Project, ProjectIndexableFilesFilter> entry : myProjectFilters.entrySet()) {
            if (entry.getValue().containsFileId(fileId)) {
                projects.add(entry.getKey());
            }
        }
        return projects;
    }

    private static <T> Supplier<T> lazy(Supplier<T> supplier) {
        return new Supplier<>() {
            private @Nullable T myValue;

            @Override
            public T get() {
                T value = myValue;
                if (value == null) {
                    value = supplier.get();
                    myValue = value;
                }
                return value;
            }
        };
    }
}
