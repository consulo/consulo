// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringProvider;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.undoRedo.UndoableAction;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class SuggestedRefactoringUndoableAction implements UndoableAction {
    private final Project myProject;
    private final TextRange mySignatureRange;
    private final String myOldDeclarationText;
    private final @Nullable String myOldImportsText;
    private final SuggestedRefactoringSupport.Signature myOldSignature;
    private final SuggestedRefactoringSupport.Signature myNewSignature;
    private final Map<String, Object> myDisappearedParameters;
    private final SuggestedRefactoringState.AdditionalData myAdditionalData;
    private final DocumentReference myDocumentReference;

    private SuggestedRefactoringUndoableAction(
        Document document,
        Project project,
        TextRange signatureRange,
        String oldDeclarationText,
        @Nullable String oldImportsText,
        SuggestedRefactoringSupport.Signature oldSignature,
        SuggestedRefactoringSupport.Signature newSignature,
        Map<String, Object> disappearedParameters,
        SuggestedRefactoringState.AdditionalData additionalData
    ) {
        myProject = project;
        mySignatureRange = signatureRange;
        myOldDeclarationText = oldDeclarationText;
        myOldImportsText = oldImportsText;
        myOldSignature = oldSignature;
        myNewSignature = newSignature;
        myDisappearedParameters = disappearedParameters;
        myAdditionalData = additionalData;
        myDocumentReference = DocumentReferenceManager.getInstance().create(document);
    }

    @RequiredReadAction
    public static SuggestedRefactoringUndoableAction create(Document document, SuggestedRefactoringState state) {
        TextRange signatureRange = Objects.requireNonNull(state.getRefactoringSupport().signatureRange(state.getAnchor()));
        return new SuggestedRefactoringUndoableAction(
            document, state.getAnchor().getProject(), signatureRange, state.getOldDeclarationText(), state.getOldImportsText(),
            state.getOldSignature(), state.getNewSignature(), state.getDisappearedParameters(), state.getAdditionalData()
        );
    }

    @Override
    public DocumentReference[] getAffectedDocuments() {
        return new DocumentReference[]{myDocumentReference};
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

    @Override
    @RequiredReadAction
    public void undo() {
        Document document = myDocumentReference.getDocument();
        if (document == null) {
            return;
        }
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);
        psiDocumentManager.commitAllDocuments();
        PsiFile psiFile = psiDocumentManager.getPsiFile(document);
        if (psiFile == null) {
            return;
        }
        SuggestedRefactoringSupport refactoringSupport = SuggestedRefactoringSupport.forLanguage(psiFile.getLanguage());
        if (refactoringSupport == null) {
            return;
        }
        PsiElement anchor = refactoringSupport.anchorByOffset(psiFile, mySignatureRange.getStartOffset());
        if (anchor == null || !mySignatureRange.equals(refactoringSupport.signatureRange(anchor))) {
            return;
        }
        SuggestedRefactoringState state = new SuggestedRefactoringState(
            anchor, refactoringSupport, SuggestedRefactoringState.ErrorLevel.NO_ERRORS,
            myOldDeclarationText, myOldImportsText, myOldSignature, myNewSignature,
            refactoringSupport.getStateChanges().parameterMarkers(anchor, myNewSignature), myDisappearedParameters,
            SuggestedRefactoringState.nextFeatureUsageId(), myAdditionalData
        );

        SuggestedRefactoringProviderImpl.getInstance(myProject).undoToState(state, mySignatureRange);
    }

    @Override
    public void redo() {
        SuggestedRefactoringProvider.getInstance(myProject).reset();
    }
}
