/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.sandboxPlugin.lang.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * The {@code #include "name"} directive — its file name is a reference to the included
 * sibling file, resolved exactly like the simulator resolves it, so navigation and the
 * context machinery always agree on the target.
 */
public class SandIncludeDirective extends ASTWrapperPsiElement {
    public SandIncludeDirective(ASTNode node) {
        super(node);
    }

    @RequiredReadAction
    public @Nullable String getIncludeName() {
        ASTNode literal = getNode().findChildByType(SandTokens.STRING_LITERAL);
        if (literal == null) {
            return null;
        }
        return literal.getText().replace("\"", "");
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiReference getReference() {
        ASTNode literal = getNode().findChildByType(SandTokens.STRING_LITERAL);
        if (literal == null) {
            return null;
        }

        int start = literal.getStartOffset() - getNode().getStartOffset();
        int length = literal.getTextLength();
        TextRange range = length > 2 ? TextRange.from(start + 1, length - 2) : TextRange.from(start, length);

        return new PsiReferenceBase<SandIncludeDirective>(this, range) {
            @RequiredReadAction
            @Override
            public @Nullable PsiElement resolve() {
                String name = getElement().getIncludeName();
                if (name == null) {
                    return null;
                }

                PsiFile psiFile = getElement().getContainingFile();
                VirtualFile file = psiFile == null ? null : psiFile.getOriginalFile().getVirtualFile();
                VirtualFile parent = file == null ? null : file.getParent();
                VirtualFile target = parent == null ? null : parent.findChild(name);
                if (target == null) {
                    return null;
                }
                return PsiManager.getInstance(getElement().getProject()).findFile(target);
            }
        };
    }
}
