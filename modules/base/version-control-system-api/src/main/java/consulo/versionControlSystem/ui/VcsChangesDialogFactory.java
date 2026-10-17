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
package consulo.versionControlSystem.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.List;

/**
 * Builds the dialogs and file lists of version control, on every frontend.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface VcsChangesDialogFactory {
    SelectFilesDialogBuilder<VirtualFile> selectFiles(Project project, List<VirtualFile> files);

    SelectFilesDialogBuilder<FilePath> selectFilePaths(Project project, List<FilePath> paths);

    ShowChangesDialogBuilder showChanges(Project project, Collection<Change> changes);

    VcsFileListBuilder<VirtualFile> fileList(Project project, List<VirtualFile> files);

    VcsFileListBuilder<FilePath> filePathList(Project project, List<FilePath> paths);

    VcsChangesBrowserBuilder changesBrowser(Project project);
}
