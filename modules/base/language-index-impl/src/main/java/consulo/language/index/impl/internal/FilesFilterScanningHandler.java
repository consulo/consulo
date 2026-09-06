// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilterHolder;
import consulo.logging.Logger;
import consulo.project.Project;

public interface FilesFilterScanningHandler {
    void addFileId(Project project, int fileId);

    default void scanningCompleted(Project project) {
        UnindexedFilesScannerStartup.setProjectFilterIsInvalidated(project, false);
    }

    void scanningStarted(Project project, boolean update);

    class UpdatingFilesFilterScanningHandler implements FilesFilterScanningHandler {
        private final ProjectIndexableFilesFilterHolder myFilterHolder;

        public UpdatingFilesFilterScanningHandler(ProjectIndexableFilesFilterHolder filterHolder) {
            myFilterHolder = filterHolder;
        }

        @Override
        public void addFileId(Project project, int fileId) {
            myFilterHolder.addFileId(fileId, project);
        }

        @Override
        public void scanningStarted(Project project, boolean isFullUpdate) {
            if (isFullUpdate) {
                myFilterHolder.resetFileIds(project);
            }
        }
    }

    class IdleFilesFilterScanningHandler implements FilesFilterScanningHandler {
        private static final Logger LOG = Logger.getInstance(IdleFilesFilterScanningHandler.class);

        @Override
        public void addFileId(Project project, int fileId) {
        }

        @Override
        public void scanningStarted(Project project, boolean update) {
            LOG.info("Scanning will happen without filling of project indexable files filter");
        }
    }
}
