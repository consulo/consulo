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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.project.ui.impl.internal.VirtualFileDeleteProvider;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.ui.SelectFilesDialogBuilder;
import consulo.versionControlSystem.ui.VcsChangesBrowserBuilder;
import consulo.versionControlSystem.ui.ShowChangesDialogBuilder;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import consulo.versionControlSystem.ui.VcsFileListBuilder;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@Singleton
@ServiceImpl
public class UnifiedVcsChangesDialogFactory implements VcsChangesDialogFactory {
    @Override
    public SelectFilesDialogBuilder<VirtualFile> selectFiles(Project project, List<VirtualFile> files) {
        return new UnifiedSelectFilesDialogBuilder<>(project, files, VcsUtil::getFilePath, VirtualFile::isValid, VirtualFileDeleteProvider::new);
    }

    @Override
    public SelectFilesDialogBuilder<FilePath> selectFilePaths(Project project, List<FilePath> paths) {
        return new UnifiedSelectFilesDialogBuilder<>(project, paths, path -> path, path -> true, null);
    }

    @Override
    public ShowChangesDialogBuilder showChanges(Project project, Collection<Change> changes) {
        return new UnifiedShowChangesDialogBuilder(project, changes);
    }

    @Override
    public VcsFileListBuilder<VirtualFile> fileList(Project project, List<VirtualFile> files) {
        return new UnifiedVcsFileListBuilder<>(project, files, VcsUtil::getFilePath, VirtualFile::isValid);
    }

    @Override
    public VcsFileListBuilder<FilePath> filePathList(Project project, List<FilePath> paths) {
        return new UnifiedVcsFileListBuilder<>(project, paths, path -> path, path -> true);
    }

    @Override
    public VcsChangesBrowserBuilder changesBrowser(Project project) {
        return new UnifiedVcsChangesBrowserBuilder(project);
    }
}
