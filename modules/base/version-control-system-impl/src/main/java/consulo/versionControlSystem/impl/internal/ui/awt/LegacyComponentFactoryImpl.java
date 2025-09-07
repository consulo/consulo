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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.impl.internal.change.ui.awt.FilePathChangesTreeListImpl;
import consulo.versionControlSystem.internal.ChangesBrowserTree;
import consulo.versionControlSystem.ui.awt.LegacyComponentFactory;
import consulo.versionControlSystem.ui.awt.LegacyDialog;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-07
 */
@ServiceImpl
@Singleton
public class LegacyComponentFactoryImpl implements LegacyComponentFactory {
    @Override
    public LegacyDialog createSelectFilePathsDialog(Project project, List<FilePath> originalFiles, String prompt, VcsShowConfirmationOption confirmationOption, @Nonnull LocalizeValue okActionName, @Nonnull LocalizeValue cancelActionName, boolean showDoNotAskOption) {
        return new SelectFilePathsDialog(project, originalFiles, prompt, confirmationOption, okActionName, cancelActionName, showDoNotAskOption);
    }

    @Override
    public LegacyDialog createSelectFilesDialog(Project project,
                                                List<VirtualFile> originalFiles,
                                                String prompt,
                                                VcsShowConfirmationOption confirmationOption,
                                                boolean selectableFiles,
                                                boolean showDoNotAskOption,
                                                boolean deletableFiles) {
        return new SelectFilesDialog(project, originalFiles, prompt, confirmationOption, selectableFiles, showDoNotAskOption, deletableFiles);
    }

    @Override
    public LegacyDialog createSelectFilesDialogOnlyOk(Project project, List<VirtualFile> originalFiles, String prompt, VcsShowConfirmationOption confirmationOption, boolean selectableFiles, boolean showDoNotAskOption, boolean deletableFiles) {
        return new SelectFilesDialog(project, originalFiles, prompt, confirmationOption, selectableFiles, showDoNotAskOption, deletableFiles) {
            {
                init();
            }
            
            @Nonnull
            @Override
            protected Action[] createActions() {
                return new Action[]{getOKAction()};
            }
        };
    }

    @Override
    public ChangesBrowserTree<VirtualFile> createVirtualFileList(Project project, List<VirtualFile> originalFiles, boolean selectableFiles, boolean deletableFiles) {
        return new SelectFilesDialog.VirtualFileListImpl(project, originalFiles, selectableFiles, deletableFiles);
    }

    @Override
    public ChangesBrowserTree<FilePath> createFilePathChangesTreeList(@Nonnull Project project, @Nonnull List<FilePath> originalFiles, boolean showCheckboxes, boolean highlightProblems, @Nullable Runnable inclusionListener) {
        return new FilePathChangesTreeListImpl(project, originalFiles, showCheckboxes, highlightProblems, inclusionListener, null);
    }

    @Override
    public LegacyDialog createChangeListViewerDialog(Project project, CommittedChangeList changeList, VirtualFile toSelect, String description) {
        return new ChangeListViewerDialog(project, changeList, toSelect) {
            @Nullable
            @Override
            protected String getDescription() {
                return description;
            }
        };
    }
}
