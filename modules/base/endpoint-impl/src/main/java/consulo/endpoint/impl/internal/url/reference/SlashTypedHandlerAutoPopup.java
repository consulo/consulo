// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.endpoint.url.reference.EnableAutopopupInUrlPathReferencesUtil;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.editor.completion.CompletionType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public final class SlashTypedHandlerAutoPopup extends TypedHandlerDelegate {
    @Override
    public Result checkAutoPopup(char charTyped, Project project, Editor editor, PsiFile file) {
        if (charTyped != '/') {
            return super.checkAutoPopup(charTyped, project, editor, file);
        }
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
            int offset = editor.getCaretModel().getOffset();
            @Nullable PsiElement psiElement = f.findElementAt(offset);
            if (psiElement == null) {
                return false;
            }
            return EnableAutopopupInUrlPathReferencesUtil.hasUsageUrlPathReferences(psiElement, offset);
        });
        return super.checkAutoPopup(charTyped, project, editor, file);
    }
}
