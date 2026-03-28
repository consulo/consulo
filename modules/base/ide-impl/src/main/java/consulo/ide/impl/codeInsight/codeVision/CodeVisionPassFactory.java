// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * Registers and creates {@link CodeVisionPass} instances.
 * <p>
 * The pass is registered to run last (after all other highlighting passes) so that
 * it can see the most up-to-date PSI state.
 */
@ExtensionImpl
public class CodeVisionPassFactory implements TextEditorHighlightingPassFactory {
    private static final Key<Long> PSI_MODIFICATION_STAMP =  Key.create("code.vision.psi.modification.stamp");

    @Override
    public void register(Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, Registrar.Anchor.LAST, -1, false);
    }

    @Override
    public @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile file, Editor editor) {
        if (editor.getEditorKind() == EditorKind.DIFF) {
            return null;
        }

        Long stamp = editor.getUserData(PSI_MODIFICATION_STAMP);
        long current = PsiModificationTracker.getInstance(file.getProject()).getModificationCount();
        if (stamp != null && stamp == current) {
            return null;
        }

        return new CodeVisionPass(file, editor);
    }

    static void updateModificationStamp(Editor editor, Project project) {
        long current = PsiModificationTracker.getInstance(project).getModificationCount();
        editor.putUserData(PSI_MODIFICATION_STAMP, current);
    }

    public static void resetModificationStamp() {
        for (Editor e : EditorFactory.getInstance().getAllEditors()) {
            resetModificationStamp(e);
        }
    }

    static void resetModificationStamp(Editor editor) {
        editor.putUserData(PSI_MODIFICATION_STAMP, null);
    }
}
