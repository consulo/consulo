// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hints.VisualFormattingLayerService;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.editor.readerMode.ReaderModeProvider;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.project.Project;

@ExtensionImpl
public class VisualFormattingLayerReaderModeProvider implements ReaderModeProvider {
    @Override
    public void applyModeChanged(Project project, Editor editor, boolean readerMode, boolean fileIsOpenAlready) {
        CodeStyleSettings settings = ReaderModeSettings.getInstance(project).getVisualFormattingCodeStyleSettings(project);
        if (readerMode && settings != null) {
            VisualFormattingLayerService.enableForEditor(editor, settings);
        }
        else {
            VisualFormattingLayerService.disableForEditor(editor);
        }
    }
}
