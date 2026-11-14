// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState.ErrorLevel;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState.ParameterMarker;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Parameter;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * A service transforming a sequence of declaration states into {@link SuggestedRefactoringState}.
 */
public abstract class SuggestedRefactoringStateChanges {
    protected final SuggestedRefactoringSupport refactoringSupport;

    protected SuggestedRefactoringStateChanges(SuggestedRefactoringSupport refactoringSupport) {
        this.refactoringSupport = refactoringSupport;
    }

    /**
     * Extracts information from declaration and stores it in an instance of {@link Signature} class.
     * <p>
     * For performance reasons, don't use any resolve in this method. More accurate information about changes can be obtained later
     * with use of {@link SuggestedRefactoringAvailability#refineSignaturesWithResolve}.
     *
     * @param anchor    declaration or call-site in its current state.
     *                  Only PsiElement's that are classified as anchors by {@link SuggestedRefactoringSupport#isAnchor} may be passed to this parameter.
     * @param prevState previous state of accumulated signature changes, or <i>null</i> if the user is just about to start editing the signature.
     * @return An instance of {@link Signature} class, representing the current state of the declaration,
     * or <i>null</i> if the declaration is in an incorrect state and no signature can be created.
     */
    @RequiredReadAction
    public abstract @Nullable Signature signature(PsiElement anchor, @Nullable SuggestedRefactoringState prevState);

    /**
     * Provides "marker ranges" for parameters in the declaration.
     * <p>
     * Marker ranges are used to keep track of parameter identity when its name changes.
     * A good marker range must have high chances of staying the same while editing the signature (with help of a {@link RangeMarker}, of course).
     * If the language has a fixed separator between parameter name and type such as ':'  - use it as a marker.
     * A whitespace between the type and the name is not so reliable because it may change its length or temporarily disappear.
     * Parameter type range is also a good marker because it's unlikely to change at the same time as the name changes.
     *
     * @param anchor declaration or call-site in its current state.
     *               Only PsiElement's that are classified as anchors by {@link SuggestedRefactoringSupport#isAnchor} may be passed to this parameter.
     * @return a list containing a marker range for each parameter, or <i>null</i> if no marker can be provided for this parameter
     */
    @RequiredReadAction
    public abstract List<@Nullable TextRange> parameterMarkerRanges(PsiElement anchor);

    @RequiredReadAction
    public @Nullable SuggestedRefactoringState createInitialState(PsiElement anchor) {
        Signature signature = signature(anchor, null);
        if (signature == null) {
            return null;
        }
        TextRange signatureRange = refactoringSupport.signatureRange(anchor);
        if (signatureRange == null) {
            return null;
        }
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(anchor.getProject());
        PsiFile file = anchor.getContainingFile();
        Document document = Objects.requireNonNull(file.getViewProvider().getDocument());
        if (!psiDocumentManager.isCommitted(document)) {
            throw new IllegalArgumentException("Document must be committed");
        }
        TextRange importsRange = refactoringSupport.importsRange(file);
        String oldImportsText = importsRange != null
            ? document.getText(TextRangeUtils.extendWithWhitespace(importsRange, document.getCharsSequence()))
            : null;
        return new SuggestedRefactoringState(
            anchor,
            refactoringSupport,
            ErrorLevel.NO_ERRORS,
            document.getText(signatureRange),
            oldImportsText,
            signature,
            signature,
            parameterMarkers(anchor, signature)
        );
    }

    /**
     * Returns a declaration for a given anchor. Returns anchor itself if it's already a declaration,
     * or a declaration if anchor is a use-site.
     *
     * @param anchor declaration or call-site in its current state.
     *               Only PsiElement's that are classified as anchors by {@link SuggestedRefactoringSupport#isAnchor} may be passed to this parameter.
     * @return found declaration. Could be null if anchor is a call-site that does not properly resolve.
     */
    @RequiredReadAction
    public @Nullable PsiElement findDeclaration(PsiElement anchor) {
        return anchor;
    }

    @RequiredReadAction
    public SuggestedRefactoringState updateState(SuggestedRefactoringState state, PsiElement anchor) {
        Signature newSignature = signature(anchor, state);
        if (newSignature == null) {
            return state.withErrorLevel(ErrorLevel.SYNTAX_ERROR);
        }

        Set<Object> idsPresent = new HashSet<>();
        for (Parameter parameter : newSignature.getParameters()) {
            idsPresent.add(parameter.getId());
        }
        Map<String, Object> disappearedParameters = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : state.getDisappearedParameters().entrySet()) {
            if (!idsPresent.contains(entry.getValue())) {
                disappearedParameters.put(entry.getKey(), entry.getValue());
            }
        }
        for (Parameter parameter : state.getNewSignature().getParameters()) {
            Object id = parameter.getId();
            if (!idsPresent.contains(id) && state.getOldSignature().parameterById(id) != null) {
                disappearedParameters.put(parameter.getName(), id); // one more parameter disappeared
            }
        }

        List<ParameterMarker> parameterMarkers = new ArrayList<>(parameterMarkers(anchor, newSignature));
        boolean syntaxError = refactoringSupport.hasSyntaxError(anchor);
        if (syntaxError) {
            // when there is a syntax error inside the signature, there can be parameters which are temporarily not parsed as parameters
            // we must keep their markers in order to match them later
            for (ParameterMarker marker : state.getParameterMarkers()) {
                if (marker.rangeMarker().isValid() && newSignature.parameterById(marker.parameterId()) == null) {
                    parameterMarkers.add(marker);
                }
            }
        }

        return state
            .withAnchor(anchor)
            .withNewSignature(newSignature)
            .withErrorLevel(syntaxError ? ErrorLevel.SYNTAX_ERROR : ErrorLevel.NO_ERRORS)
            .withParameterMarkers(parameterMarkers)
            .withDisappearedParameters(disappearedParameters);
    }

    @RequiredReadAction
    protected Signature matchParametersWithPrevState(
        Signature signature,
        PsiElement newDeclaration,
        SuggestedRefactoringState prevState
    ) {
        // first match all parameters by names (in prevState or in the history of changes)
        List<Parameter> parameters = signature.getParameters();
        List<@Nullable Object> ids = new ArrayList<>(parameters.size());
        for (Parameter parameter : parameters) {
            ids.add(guessParameterIdByName(parameter, prevState));
        }

        // now match those that we could not match by name via marker ranges
        List<@Nullable TextRange> markerRanges = parameterMarkerRanges(newDeclaration);
        for (int index = 0; index < parameters.size(); index++) {
            TextRange markerRange = markerRanges.get(index);
            if (ids.get(index) == null && markerRange != null) {
                Object id = guessParameterIdByMarkers(markerRange, prevState);
                if (id != null && !ids.contains(id)) {
                    ids.set(index, id);
                }
            }
        }

        List<Parameter> newParameters = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            Object id = ids.get(i);
            newParameters.add(parameters.get(i).withId(id != null ? id : new Object()/*new id*/));
        }
        return Objects.requireNonNull(
            Signature.create(signature.getName(), signature.getType(), newParameters, signature.getAdditionalData())
        );
    }

    private static @Nullable Object guessParameterIdByName(Parameter parameter, SuggestedRefactoringState prevState) {
        Parameter byName = prevState.getNewSignature().parameterByName(parameter.getName());
        if (byName != null) {
            return byName.getId();
        }

        return prevState.getDisappearedParameters().get(parameter.getName());
    }

    protected @Nullable Object guessParameterIdByMarkers(TextRange markerRange, SuggestedRefactoringState prevState) {
        for (ParameterMarker marker : prevState.getParameterMarkers()) {
            if (markerRange.equals(TextRangeUtils.asTextRange(marker.rangeMarker()))) {
                return marker.parameterId();
            }
        }
        return null;
    }

    @RequiredReadAction
    public List<ParameterMarker> parameterMarkers(PsiElement declaration, Signature signature) {
        Document document = Objects.requireNonNull(declaration.getContainingFile().getViewProvider().getDocument());
        List<@Nullable TextRange> markerRanges = parameterMarkerRanges(declaration);
        if (markerRanges.size() != signature.getParameters().size()) {
            throw new IllegalArgumentException("Marker ranges count must match parameters count");
        }
        List<ParameterMarker> result = new ArrayList<>();
        for (int i = 0; i < markerRanges.size(); i++) {
            TextRange range = markerRanges.get(i);
            if (range != null) {
                result.add(new ParameterMarker(document.createRangeMarker(range), signature.getParameters().get(i).getId()));
            }
        }
        return result;
    }

    /**
     * Use this implementation of {@link SuggestedRefactoringStateChanges}, if only Rename refactoring is supported for the language.
     */
    public static class RenameOnly extends SuggestedRefactoringStateChanges {
        public RenameOnly(SuggestedRefactoringSupport refactoringSupport) {
            super(refactoringSupport);
        }

        @RequiredReadAction
        @Override
        public @Nullable Signature signature(PsiElement anchor, @Nullable SuggestedRefactoringState prevState) {
            String name = anchor instanceof PsiNamedElement namedElement ? namedElement.getName() : null;
            if (name == null) {
                return null;
            }

            return Objects.requireNonNull(Signature.create(name, null, List.of(), null));
        }

        @RequiredReadAction
        @Override
        public List<@Nullable TextRange> parameterMarkerRanges(PsiElement anchor) {
            return List.of();
        }
    }
}
