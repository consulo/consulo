/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.ui.awt;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.internal.ChangesBrowserTree;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-07
 */
@ServiceAPI(ComponentScope.APPLICATION)
@UsedInPlugin
public interface LegacyComponentFactory {
    LegacyDialog createSelectFilePathsDialog(Project project,
                                             List<FilePath> originalFiles,
                                             String prompt,
                                             VcsShowConfirmationOption confirmationOption,
                                             @Nonnull LocalizeValue okActionName,
                                             @Nonnull LocalizeValue cancelActionName,
                                             boolean showDoNotAskOption);

    LegacyDialog createSelectFilesDialog(Project project,
                                         List<VirtualFile> originalFiles,
                                         String prompt,
                                         VcsShowConfirmationOption confirmationOption,
                                         boolean selectableFiles,
                                         boolean showDoNotAskOption,
                                         boolean deletableFiles);

    LegacyDialog createSelectFilesDialogOnlyOk(Project project,
                                         List<VirtualFile> originalFiles,
                                         String prompt,
                                         VcsShowConfirmationOption confirmationOption,
                                         boolean selectableFiles,
                                         boolean showDoNotAskOption,
                                         boolean deletableFiles);

    ChangesBrowserTree<VirtualFile> createVirtualFileList(Project project,
                                                          List<VirtualFile> originalFiles,
                                                          boolean selectableFiles,
                                                          boolean deletableFiles);

    ChangesBrowserTree<FilePath> createFilePathChangesTreeList(@Nonnull Project project,
                                                               @Nonnull List<FilePath> originalFiles,
                                                               boolean showCheckboxes,
                                                               boolean highlightProblems,
                                                               @Nullable Runnable inclusionListener);

    LegacyDialog createChangeListViewerDialog(Project project, CommittedChangeList changeList, VirtualFile toSelect, String description);
}
