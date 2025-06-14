// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.impl.internal.inlay.param.MethodInfoExcludeListFilter;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class ParameterHintsPassFactory implements TextEditorHighlightingPassFactory {
    private static final Key<Long> PSI_MODIFICATION_STAMP = Key.create("psi.modification.stamp");

    @Override
    public void register(@Nonnull Registrar registrar) {
        boolean serialized = false;
        int[] ghl = serialized ? new int[]{Pass.UPDATE_ALL} : null;
        registrar.registerTextEditorHighlightingPass(this, ghl, null, false, -1);
    }

    @Override
    @Nullable
    @RequiredReadAction
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile psiFile, @Nonnull Editor editor) {
        if (editor.isOneLineMode()) return null;
        long currentStamp = getCurrentModificationStamp(psiFile);
        Long savedStamp = editor.getUserData(PSI_MODIFICATION_STAMP);
        if (savedStamp != null && savedStamp == currentStamp) return null;
        Language language = psiFile.getLanguage();
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider == null) return null;
        return new ParameterHintsPass(psiFile, editor, MethodInfoExcludeListFilter.forLanguage(language), false);
    }

    public static long getCurrentModificationStamp(@Nonnull PsiFile file) {
        return file.getManager().getModificationTracker().getModificationCount();
    }

    public static void forceHintsUpdateOnNextPass() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            forceHintsUpdateOnNextPass(editor);
        }
    }

    public static void forceHintsUpdateOnNextPass(@Nonnull Editor editor) {
        editor.putUserData(PSI_MODIFICATION_STAMP, null);
    }

    static void putCurrentPsiModificationStamp(@Nonnull Editor editor, @Nonnull PsiFile file) {
        editor.putUserData(PSI_MODIFICATION_STAMP, getCurrentModificationStamp(file));
    }
}