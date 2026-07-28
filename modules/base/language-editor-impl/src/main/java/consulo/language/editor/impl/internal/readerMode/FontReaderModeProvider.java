// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorScrollingPositionKeeper;
import consulo.colorScheme.EditorColorsManager;
import consulo.language.editor.readerMode.ReaderModeProvider;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.project.Project;

@ExtensionImpl
public class FontReaderModeProvider implements ReaderModeProvider {
    @Override
    public void applyModeChanged(Project project, Editor editor, boolean readerMode, boolean fileIsOpenAlready) {
        float lineSpacing = EditorColorsManager.getInstance().getGlobalScheme().getLineSpacing();
        setLineSpacing(
            editor,
            readerMode && ReaderModeSettings.getInstance(project).isIncreaseLineSpacing() ? lineSpacing * 1.2f : lineSpacing
        );
    }

    private static void setLineSpacing(Editor editor, float lineSpacing) {
        EditorScrollingPositionKeeper.perform(editor, false, () -> editor.getColorsScheme().setLineSpacing(lineSpacing));
    }
}
