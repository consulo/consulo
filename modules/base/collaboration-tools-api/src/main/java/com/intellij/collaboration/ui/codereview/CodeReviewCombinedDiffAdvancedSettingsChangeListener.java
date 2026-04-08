// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.file.codereview.CodeReviewDiffVirtualFile;
import consulo.project.Project;
import consulo.project.ProjectManager;
import jakarta.annotation.Nonnull;

final class CodeReviewCombinedDiffAdvancedSettingsChangeListener implements AdvancedSettingsChangeListener {
    @Override
    public void advancedSettingChanged(@Nonnull String id, @Nonnull Object oldValue, @Nonnull Object newValue) {
        if (id.equals(CodeReviewAdvancedSettings.COMBINED_DIFF_SETTING_ID)) {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                DiffEditorViewerFileEditor.reloadDiffEditorsForFiles(project, file -> file instanceof CodeReviewDiffVirtualFile);
            }
        }
    }
}
