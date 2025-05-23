// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.stickyLine;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.StickyLinesCollector;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class StickyLinesPassFactory implements TextEditorHighlightingPassFactory, DumbAware {

    @Override
    public void register(@Nonnull Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Override
    public TextEditorHighlightingPass createHighlightingPass(PsiFile psiFile, Editor editor) {
        if (editor.getProject() != null &&
            !editor.isDisposed() &&
            editor.getSettings().isStickyLineShown()) {
            if (StickyLinesCollector.ModStamp.isChanged(editor, psiFile)) {
                return new StickyLinesPass(editor, psiFile);
            }
        }
        return null;
    }
}
