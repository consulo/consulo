// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

/**
 * Registers and creates {@link CodeVisionPass} instances.
 * <p>
 * The pass is registered to run last (after all other highlighting passes) so that
 * it can see the most up-to-date PSI state.
 */
@ExtensionImpl
public class CodeVisionPassFactory implements TextEditorHighlightingPassFactory {
    @Override
    public void register(Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, Registrar.Anchor.LAST, -1, false);
    }

    @Override
    public @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile file, Editor editor) {
        return new CodeVisionPass(file, editor);
    }
}
