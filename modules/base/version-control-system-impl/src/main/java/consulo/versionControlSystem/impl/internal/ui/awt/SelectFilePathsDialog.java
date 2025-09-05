/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesTreeList;
import consulo.versionControlSystem.impl.internal.change.ui.awt.FilePathChangesTreeList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author yole
 */
public class SelectFilePathsDialog extends AbstractSelectFilesDialog<FilePath> {
    private final ChangesTreeList<FilePath> myFileList;

    public SelectFilePathsDialog(
        Project project,
        List<FilePath> originalFiles,
        String prompt,
        VcsShowConfirmationOption confirmationOption,
        @Nonnull LocalizeValue okActionName,
        @Nonnull LocalizeValue cancelActionName,
        boolean showDoNotAskOption
    ) {
        super(project, false, confirmationOption, prompt, showDoNotAskOption);
        myFileList = new FilePathChangesTreeList(project, originalFiles, true, true, null, null);
        if (okActionName != LocalizeValue.empty()) {
            getOKAction().setText(okActionName);
        }
        if (cancelActionName != LocalizeValue.empty()) {
            getCancelAction().setText(cancelActionName);
        }
        myFileList.setChangesToDisplay(originalFiles);
        init();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public SelectFilePathsDialog(
        Project project,
        List<FilePath> originalFiles,
        String prompt,
        VcsShowConfirmationOption confirmationOption,
        @Nullable String okActionName,
        @Nullable String cancelActionName,
        boolean showDoNotAskOption
    ) {
        super(project, false, confirmationOption, prompt, showDoNotAskOption);
        myFileList = new FilePathChangesTreeList(project, originalFiles, true, true, null, null);
        if (okActionName != null) {
            getOKAction().putValue(Action.NAME, okActionName);
        }
        if (cancelActionName != null) {
            getCancelAction().putValue(Action.NAME, cancelActionName);
        }
        myFileList.setChangesToDisplay(originalFiles);
        init();
    }

    public List<FilePath> getSelectedFiles() {
        return List.copyOf(myFileList.getIncludedChanges());
    }

    @Nonnull
    @Override
    protected ChangesTreeList getFileList() {
        return myFileList;
    }
}
