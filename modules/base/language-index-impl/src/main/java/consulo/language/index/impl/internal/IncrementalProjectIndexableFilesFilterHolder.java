// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.language.index.impl.internal;

import consulo.application.ApplicationManager;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

class IncrementalProjectIndexableFilesFilterHolder extends ProjectIndexableFilesFilterHolder {
    private final ConcurrentMap<Project, IncrementalProjectIndexableFilesFilter> myProjectFilters = new ConcurrentHashMap<>();

    IncrementalProjectIndexableFilesFilterHolder() {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosed(Project project, UIAccess uiAccess) {
                myProjectFilters.remove(project);
            }
        });
    }

    @Override
    @Nullable IdFilter getProjectIndexableFiles(Project project) {
        if (!UnindexedFilesScanner.isProjectContentFullyScanned(project) || UnindexedFilesScanner.isIndexUpdateInProgress(project)) {
            return null;
        }
        return getFilter(project);
    }

    @Override
    void entireProjectUpdateStarted(Project project) {
        assert UnindexedFilesScanner.isIndexUpdateInProgress(project);

        getFilter(project).memoizeAndResetFileIds();
    }

    @Override
    void entireProjectUpdateFinished(Project project) {
        assert UnindexedFilesScanner.isIndexUpdateInProgress(project);

        getFilter(project).resetPreviousFileIds();
    }

    @Override
    void addFileId(int fileId, Supplier<Set<Project>> projects) {
        Supplier<Set<Project>> matchedProjects = lazy(projects);
        for (Map.Entry<Project, IncrementalProjectIndexableFilesFilter> entry : myProjectFilters.entrySet()) {
            entry.getValue().ensureFileIdPresent(fileId, () -> matchedProjects.get().contains(entry.getKey()));
        }
    }

    @Override
    boolean addFileId(int fileId, Project project) {
        return getFilter(project).ensureFileIdPresent(fileId, () -> true);
    }

    @Override
    void removeFile(int fileId) {
        for (IncrementalProjectIndexableFilesFilter filter : myProjectFilters.values()) {
            filter.removeFileId(fileId);
        }
    }

    private IncrementalProjectIndexableFilesFilter getFilter(Project project) {
        return myProjectFilters.computeIfAbsent(project, p -> new IncrementalProjectIndexableFilesFilter());
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
