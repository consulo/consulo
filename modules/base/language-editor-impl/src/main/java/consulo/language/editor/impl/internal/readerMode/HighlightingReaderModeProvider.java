// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.readerMode.ReaderModeProvider;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

@ExtensionImpl
public class HighlightingReaderModeProvider implements ReaderModeProvider {
    @Override
    public void applyModeChanged(Project project, Editor editor, boolean readerMode, boolean fileIsOpenAlready) {
        if (!fileIsOpenAlready) {
            return;
        }

        FileHighlightingSetting highlighting = readerMode && !ReaderModeSettings.getInstance(project).isShowWarnings()
            ? FileHighlightingSetting.SKIP_INSPECTION
            : FileHighlightingSetting.FORCE_HIGHLIGHTING;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return;
        }
        HighlightLevelUtil.forceRootHighlighting(psiFile, highlighting);
    }
}
