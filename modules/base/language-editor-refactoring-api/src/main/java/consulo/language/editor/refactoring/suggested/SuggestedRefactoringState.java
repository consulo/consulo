// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data class representing the state of accumulated signature changes.
 */
public final class SuggestedRefactoringState {
    private static final AtomicInteger ourNextFeatureUsageId = new AtomicInteger();

    private final PsiElement myAnchor;
    private final SuggestedRefactoringSupport myRefactoringSupport;
    private final ErrorLevel myErrorLevel;
    private final String myOldDeclarationText;
    private final @Nullable String myOldImportsText;
    private final Signature myOldSignature;
    private final Signature myNewSignature;
    private final List<ParameterMarker> myParameterMarkers;
    /* last known parameter name to its id */
    private final Map<String, Object> myDisappearedParameters;
    private final int myFeatureUsageId;
    private final AdditionalData myAdditionalData;

    private @Nullable PsiElement myRestoredDeclarationCopy;

    public SuggestedRefactoringState(
        PsiElement anchor,
        SuggestedRefactoringSupport refactoringSupport,
        ErrorLevel errorLevel,
        String oldDeclarationText,
        @Nullable String oldImportsText,
        Signature oldSignature,
        Signature newSignature,
        List<ParameterMarker> parameterMarkers
    ) {
        this(
            anchor, refactoringSupport, errorLevel, oldDeclarationText, oldImportsText, oldSignature, newSignature, parameterMarkers,
            Map.of(), ourNextFeatureUsageId.getAndIncrement(), AdditionalData.EMPTY
        );
    }

    public SuggestedRefactoringState(
        PsiElement anchor,
        SuggestedRefactoringSupport refactoringSupport,
        ErrorLevel errorLevel,
        String oldDeclarationText,
        @Nullable String oldImportsText,
        Signature oldSignature,
        Signature newSignature,
        List<ParameterMarker> parameterMarkers,
        Map<String, Object> disappearedParameters,
        int featureUsageId,
        AdditionalData additionalData
    ) {
        myAnchor = anchor;
        myRefactoringSupport = refactoringSupport;
        myErrorLevel = errorLevel;
        myOldDeclarationText = oldDeclarationText;
        myOldImportsText = oldImportsText;
        myOldSignature = oldSignature;
        myNewSignature = newSignature;
        myParameterMarkers = parameterMarkers;
        myDisappearedParameters = disappearedParameters;
        myFeatureUsageId = featureUsageId;
        myAdditionalData = additionalData;
    }

    public static int nextFeatureUsageId() {
        return ourNextFeatureUsageId.getAndIncrement();
    }

    public PsiElement getAnchor() {
        return myAnchor;
    }

    public SuggestedRefactoringSupport getRefactoringSupport() {
        return myRefactoringSupport;
    }

    public ErrorLevel getErrorLevel() {
        return myErrorLevel;
    }

    public String getOldDeclarationText() {
        return myOldDeclarationText;
    }

    public @Nullable String getOldImportsText() {
        return myOldImportsText;
    }

    public Signature getOldSignature() {
        return myOldSignature;
    }

    public Signature getNewSignature() {
        return myNewSignature;
    }

    public List<ParameterMarker> getParameterMarkers() {
        return myParameterMarkers;
    }

    public Map<String, Object> getDisappearedParameters() {
        return myDisappearedParameters;
    }

    public int getFeatureUsageId() {
        return myFeatureUsageId;
    }

    public AdditionalData getAdditionalData() {
        return myAdditionalData;
    }

    /**
     * Returns the declaration that should be refactored. May differ from anchor if refactoring is started from usage.
     * Use with care, as this property may involve symbol resolution.
     */
    @RequiredReadAction
    public @Nullable PsiElement getDeclaration() {
        return myRefactoringSupport.getStateChanges().findDeclaration(myAnchor);
    }

    public SuggestedRefactoringState withAnchor(PsiElement anchor) {
        return copy(
            anchor, myErrorLevel, myOldSignature, myNewSignature, myParameterMarkers, myDisappearedParameters, myAdditionalData
        );
    }

    public SuggestedRefactoringState withErrorLevel(ErrorLevel errorLevel) {
        return copy(
            myAnchor, errorLevel, myOldSignature, myNewSignature, myParameterMarkers, myDisappearedParameters, myAdditionalData
        );
    }

    public SuggestedRefactoringState withOldSignature(Signature oldSignature) {
        return copy(
            myAnchor, myErrorLevel, oldSignature, myNewSignature, myParameterMarkers, myDisappearedParameters, myAdditionalData
        );
    }

    public SuggestedRefactoringState withNewSignature(Signature newSignature) {
        return copy(
            myAnchor, myErrorLevel, myOldSignature, newSignature, myParameterMarkers, myDisappearedParameters, myAdditionalData
        );
    }

    public SuggestedRefactoringState withParameterMarkers(List<ParameterMarker> parameterMarkers) {
        return copy(
            myAnchor, myErrorLevel, myOldSignature, myNewSignature, parameterMarkers, myDisappearedParameters, myAdditionalData
        );
    }

    public SuggestedRefactoringState withDisappearedParameters(Map<String, Object> disappearedParameters) {
        return copy(
            myAnchor, myErrorLevel, myOldSignature, myNewSignature, myParameterMarkers, disappearedParameters, myAdditionalData
        );
    }

    public <T> SuggestedRefactoringState withAdditionalData(Key<T> key, T value) {
        return copy(
            myAnchor, myErrorLevel, myOldSignature, myNewSignature, myParameterMarkers, myDisappearedParameters,
            myAdditionalData.withData(key, value)
        );
    }

    private SuggestedRefactoringState copy(
        PsiElement anchor,
        ErrorLevel errorLevel,
        Signature oldSignature,
        Signature newSignature,
        List<ParameterMarker> parameterMarkers,
        Map<String, Object> disappearedParameters,
        AdditionalData additionalData
    ) {
        SuggestedRefactoringState state = new SuggestedRefactoringState(
            anchor, myRefactoringSupport, errorLevel, myOldDeclarationText, myOldImportsText,
            oldSignature, newSignature, parameterMarkers, disappearedParameters, myFeatureUsageId, additionalData
        );
        copyRestoredDeclaration(state);
        return state;
    }

    private void copyRestoredDeclaration(SuggestedRefactoringState toState) {
        if (toState.myAnchor == myAnchor) {
            // do not reset lazy calculated anchor copy for performance reasons
            toState.myRestoredDeclarationCopy = myRestoredDeclarationCopy;
        }
    }

    @RequiredReadAction
    public @Nullable PsiElement restoredDeclarationCopy() {
        if (myErrorLevel == ErrorLevel.INCONSISTENT) {
            throw new IllegalArgumentException("restoredDeclarationCopy() should not be invoked for inconsistent state");
        }
        PsiElement decl = getDeclaration();
        if (decl != myAnchor) {
            return decl;
        }
        if (myRestoredDeclarationCopy == null) {
            myRestoredDeclarationCopy = createRestoredDeclarationCopy();
        }
        return myRestoredDeclarationCopy;
    }

    @RequiredReadAction
    private PsiElement createRestoredDeclarationCopy() {
        if (!myAnchor.isValid()) {
            throw new ProcessCanceledException();
        }
        PsiFile psiFile = myAnchor.getContainingFile();
        TextRange signatureRange = myRefactoringSupport.signatureRange(myAnchor);
        if (signatureRange == null) {
            throw new ProcessCanceledException();
        }
        TextRange importsRange = myRefactoringSupport.importsRange(psiFile);
        if (importsRange != null && importsRange.getLength() != 0) {
            if (importsRange.getEndOffset() >= signatureRange.getStartOffset()) {
                throw new IllegalArgumentException("Imports range " + importsRange + " must be before signature " + signatureRange);
            }
        }

        PsiFile fileCopy = (PsiFile) psiFile.copy();
        Document document = Objects.requireNonNull(fileCopy.getViewProvider().getDocument());
        document.replaceString(signatureRange.getStartOffset(), signatureRange.getEndOffset(), myOldDeclarationText);
        if (myOldImportsText != null) {
            Objects.requireNonNull(importsRange);
            document.replaceString(importsRange.getStartOffset(), importsRange.getEndOffset(), myOldImportsText);
        }
        PsiDocumentManager.getInstance(psiFile.getProject()).commitDocument(document);

        int originalSignatureStart = signatureRange.getStartOffset();
        if (myOldImportsText != null) {
            originalSignatureStart -= importsRange.getLength();
            originalSignatureStart += myOldImportsText.length();
        }

        return Objects.requireNonNull(myRefactoringSupport.anchorByOffset(fileCopy, originalSignatureStart));
    }

    public record ParameterMarker(RangeMarker rangeMarker, Object parameterId) {
    }

    public static final class AdditionalData {
        public static final AdditionalData EMPTY = new AdditionalData(Map.of());

        private final Map<Key<?>, Object> myMap;

        private AdditionalData(Map<Key<?>, Object> map) {
            myMap = map;
        }

        @SuppressWarnings("unchecked")
        public <T> @Nullable T get(Key<T> key) {
            return (T) myMap.get(key);
        }

        public <T> AdditionalData withData(Key<T> key, T data) {
            Map<Key<?>, Object> map = new HashMap<>(myMap);
            map.put(key, data);
            return new AdditionalData(Map.copyOf(map));
        }
    }

    public enum ErrorLevel {
        /**
         * No syntax errors and refactoring may be suggested (if it makes sense)
         */
        NO_ERRORS,
        /**
         * There is a syntax error in the signature or duplicated parameter names in the signature
         */
        SYNTAX_ERROR,
        /**
         * The state is inconsistent: declaration is invalid, or signature range marker does not match signature range anymore
         */
        INCONSISTENT
    }
}
