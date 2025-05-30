/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class NonCodeUsageInfo extends MoveRenameUsageInfo {
    public final String newText;

    @RequiredReadAction
    private NonCodeUsageInfo(@Nonnull PsiElement element, int startOffset, int endOffset, PsiElement referencedElement, String newText) {
        super(element, null, startOffset, endOffset, referencedElement, true);
        this.newText = newText;
    }

    @Nullable
    @RequiredReadAction
    public static NonCodeUsageInfo create(
        @Nonnull PsiFile file,
        int startOffset,
        int endOffset,
        PsiElement referencedElement,
        String newText
    ) {
        PsiElement element = file.findElementAt(startOffset);
        while (element != null) {
            TextRange range = element.getTextRange();
            if (range.getEndOffset() < endOffset) {
                element = element.getParent();
            }
            else {
                break;
            }
        }

        if (element == null) {
            return null;
        }

        int elementStart = element.getTextRange().getStartOffset();
        startOffset -= elementStart;
        endOffset -= elementStart;
        return new NonCodeUsageInfo(element, startOffset, endOffset, referencedElement, newText);
    }

    @Override
    @Nullable
    @RequiredReadAction
    public PsiReference getReference() {
        return null;
    }

    @RequiredReadAction
    public NonCodeUsageInfo replaceElement(PsiElement newElement) {
        return new NonCodeUsageInfo(
            newElement,
            getRangeInElement().getStartOffset(),
            getRangeInElement().getEndOffset(),
            getReferencedElement(),
            newText
        );
    }
}
