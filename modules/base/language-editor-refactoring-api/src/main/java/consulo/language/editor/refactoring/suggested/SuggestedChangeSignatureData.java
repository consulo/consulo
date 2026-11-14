// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Data representing suggested Change Signature refactoring.
 */
public final class SuggestedChangeSignatureData extends SuggestedRefactoringData {
    private final SmartPsiElementPointer<PsiElement> myAnchorPointer;
    private final SmartPsiElementPointer<PsiElement> myDeclarationPointer;
    private final Signature myOldSignature;
    private final Signature myNewSignature;
    private final LocalizeValue myNameOfStuffToUpdate;
    private final String myOldDeclarationText;
    private final @Nullable String myOldImportsText;

    public SuggestedChangeSignatureData(
        SmartPsiElementPointer<PsiElement> anchorPointer,
        SmartPsiElementPointer<PsiElement> declarationPointer,
        Signature oldSignature,
        Signature newSignature,
        LocalizeValue nameOfStuffToUpdate,
        String oldDeclarationText,
        @Nullable String oldImportsText
    ) {
        myAnchorPointer = anchorPointer;
        myDeclarationPointer = declarationPointer;
        myOldSignature = oldSignature;
        myNewSignature = newSignature;
        myNameOfStuffToUpdate = nameOfStuffToUpdate;
        myOldDeclarationText = oldDeclarationText;
        myOldImportsText = oldImportsText;
    }

    public SmartPsiElementPointer<PsiElement> getAnchorPointer() {
        return myAnchorPointer;
    }

    public SmartPsiElementPointer<PsiElement> getDeclarationPointer() {
        return myDeclarationPointer;
    }

    public Signature getOldSignature() {
        return myOldSignature;
    }

    public Signature getNewSignature() {
        return myNewSignature;
    }

    public LocalizeValue getNameOfStuffToUpdate() {
        return myNameOfStuffToUpdate;
    }

    public String getOldDeclarationText() {
        return myOldDeclarationText;
    }

    public @Nullable String getOldImportsText() {
        return myOldImportsText;
    }

    @Override
    public PsiElement getDeclaration() {
        return Objects.requireNonNull(myDeclarationPointer.getElement());
    }

    public PsiElement getAnchor() {
        return Objects.requireNonNull(myAnchorPointer.getElement());
    }

    public SuggestedChangeSignatureData withNewSignature(Signature newSignature) {
        return new SuggestedChangeSignatureData(
            myAnchorPointer, myDeclarationPointer, myOldSignature, newSignature, myNameOfStuffToUpdate, myOldDeclarationText, myOldImportsText
        );
    }

    @Override
    public LocalizeValue getIntentionText() {
        return RefactoringLocalize.suggestedRefactoringChangeSignatureIntentionText(myNameOfStuffToUpdate);
    }

    @RequiredWriteAction
    public Runnable restoreInitialState(SuggestedRefactoringSupport refactoringSupport) {
        PsiFile file = Objects.requireNonNull(myAnchorPointer.getContainingFile());
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(file.getProject());
        Document document = Objects.requireNonNull(psiDocumentManager.getDocument(file));
        if (!psiDocumentManager.isCommitted(document)) {
            throw new IllegalArgumentException("Document must be committed");
        }

        TextRange signatureRange = Objects.requireNonNull(refactoringSupport.signatureRange(getAnchor()));
        TextRange importsRange = refactoringSupport.importsRange(file);
        if (importsRange != null) {
            importsRange = TextRangeUtils.extendWithWhitespace(importsRange, document.getCharsSequence());
        }

        String newSignatureText = document.getText(signatureRange);
        String newImportsText = importsRange != null ? document.getText(importsRange) : null;
        if (newImportsText != null && newImportsText.equals(myOldImportsText)) {
            newImportsText = null;
        }

        document.replaceString(signatureRange.getStartOffset(), signatureRange.getEndOffset(), myOldDeclarationText);
        if (newImportsText != null) {
            document.replaceString(importsRange.getStartOffset(), importsRange.getEndOffset(), Objects.requireNonNull(myOldImportsText));
        }
        psiDocumentManager.commitDocument(document);

        String finalNewImportsText = newImportsText;
        return () -> {
            if (!psiDocumentManager.isCommitted(document)) {
                throw new IllegalArgumentException("Document must be committed");
            }

            TextRange newSignatureRange = Objects.requireNonNull(refactoringSupport.signatureRange(getAnchor()));
            document.replaceString(newSignatureRange.getStartOffset(), newSignatureRange.getEndOffset(), newSignatureText);

            if (finalNewImportsText != null) {
                TextRange newImportsRange = TextRangeUtils.extendWithWhitespace(
                    Objects.requireNonNull(refactoringSupport.importsRange(file)),
                    document.getCharsSequence()
                );
                document.replaceString(newImportsRange.getStartOffset(), newImportsRange.getEndOffset(), finalNewImportsText);
            }

            psiDocumentManager.commitDocument(document);
        };
    }

    /**
     * Creates an instance of {@link SuggestedChangeSignatureData}.
     *
     * @param nameOfStuffToUpdate term to be used in the UI to describe usages to be updated.
     */
    @RequiredReadAction
    public static SuggestedChangeSignatureData create(SuggestedRefactoringState state, LocalizeValue nameOfStuffToUpdate) {
        return new SuggestedChangeSignatureData(
            SmartPointerManager.createPointer(state.getAnchor()),
            SmartPointerManager.createPointer(Objects.requireNonNull(state.getDeclaration())),
            state.getOldSignature(),
            state.getNewSignature(),
            nameOfStuffToUpdate,
            state.getOldDeclarationText(),
            state.getOldImportsText()
        );
    }
}
