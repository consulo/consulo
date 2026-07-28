// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.DefaultHighlightingSettingProvider;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ReaderModeHighlightingSettingsProvider extends DefaultHighlightingSettingProvider implements DumbAware {
    @RequiredReadAction
    @Override
    public @Nullable FileHighlightingSetting getDefaultSetting(Project project, VirtualFile file) {
        ReaderModeSettings readerModeSettings = ReaderModeSettings.getInstance(project);
        if (readerModeSettings.isEnabled()
            && !readerModeSettings.isShowWarnings()
            && file.isValid()
            && ReaderModeSettings.matchMode(project, file)) {
            return FileHighlightingSetting.SKIP_INSPECTION;
        }
        return null;
    }
}
