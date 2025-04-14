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
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.psi.*;
import consulo.project.Project;

import jakarta.annotation.Nullable;

public class MoveRenameUsageInfo extends UsageInfo {
    private SmartPsiElementPointer myReferencedElementPointer = null;
    private PsiElement myReferencedElement;

    private PsiReference myReference;
    private RangeMarker myReferenceRangeMarker = null;

    @RequiredReadAction
    public MoveRenameUsageInfo(PsiReference reference, PsiElement referencedElement) {
        this(reference.getElement(), reference, referencedElement);
    }

    @RequiredReadAction
    public MoveRenameUsageInfo(PsiElement element, PsiReference reference, PsiElement referencedElement) {
        super(element);
        init(element, reference, referencedElement);
    }

    @RequiredReadAction
    public MoveRenameUsageInfo(
        PsiElement element,
        PsiReference reference,
        int startOffset,
        int endOffset,
        PsiElement referencedElement,
        boolean nonCodeUsage
    ) {
        super(element, startOffset, endOffset, nonCodeUsage);
        init(element, reference, referencedElement);
    }

    @RequiredReadAction
    private void init(PsiElement element, PsiReference reference, PsiElement referencedElement) {
        Project project = element.getProject();
        myReferencedElement = referencedElement;
        if (referencedElement != null) {
            myReferencedElementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(referencedElement);
        }
        if (reference == null) {
            reference = element.getReference();
        }
        PsiFile containingFile = element.getContainingFile();
        if (reference == null) {
            TextRange textRange = element.getTextRange();
            if (textRange != null) {
                reference = containingFile.findReferenceAt(textRange.getStartOffset());
            }
        }
        myReference = reference;
        if (reference != null) {
            Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
            if (document != null) {
                int elementStart = reference.getElement().getTextRange().getStartOffset();
                myReferenceRangeMarker = document.createRangeMarker(
                    elementStart + reference.getRangeInElement().getStartOffset(),
                    elementStart + reference.getRangeInElement().getEndOffset()
                );
            }
            myDynamicUsage = reference.resolve() == null;
        }
    }

    @Nullable
    @RequiredReadAction
    public PsiElement getUpToDateReferencedElement() {
        return myReferencedElementPointer == null ? null : myReferencedElementPointer.getElement();
    }

    @Nullable
    public PsiElement getReferencedElement() {
        return myReferencedElement;
    }

    @Override
    @Nullable
    @RequiredReadAction
    public PsiReference getReference() {
        if (myReference != null) {
            PsiElement element = myReference.getElement();
            if (element != null && element.isValid()) {
                return myReference;
            }
        }

        if (myReferenceRangeMarker == null) {
            return null;
        }
        PsiElement element = getElement();
        if (element == null) {
            return null;
        }
        int start = myReferenceRangeMarker.getStartOffset() - element.getTextRange().getStartOffset();
        int end = myReferenceRangeMarker.getEndOffset() - element.getTextRange().getStartOffset();
        PsiReference reference = element.findReferenceAt(start);
        if (reference == null) {
            return null;
        }
        TextRange rangeInElement = reference.getRangeInElement();
        if (rangeInElement.getStartOffset() != start || rangeInElement.getEndOffset() != end) {
            return null;
        }
        return reference;
    }
}
