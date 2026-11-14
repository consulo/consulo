// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Parameter;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A service determining available refactoring for a given {@link SuggestedRefactoringState}.
 */
public abstract class SuggestedRefactoringAvailability {
    protected final SuggestedRefactoringSupport refactoringSupport;

    protected SuggestedRefactoringAvailability(SuggestedRefactoringSupport refactoringSupport) {
        this.refactoringSupport = refactoringSupport;
    }

    /**
     * Detects if we should suppress refactoring suggestion for this declaration.
     * <p>
     * This method is supposed to use {@link SuggestedRefactoringState#restoredDeclarationCopy} in order to analyze the original declaration.
     * It's allowed to use reference resolve in this method.
     * If resolve is not needed then it's recommended to override {@link SuggestedRefactoringStateChanges#createInitialState} and do the checks there.
     * This method should not be called if {@link SuggestedRefactoringState#getErrorLevel()} == {@link SuggestedRefactoringState.ErrorLevel#INCONSISTENT}.
     *
     * @return true, if the refactoring suggestion should be permanently disabled for this declaration
     * and all further changes in the signature ignored.
     */
    @RequiredReadAction
    public boolean shouldSuppressRefactoringForDeclaration(SuggestedRefactoringState state) {
        return false;
    }

    /**
     * Refines the old and the new signatures with use of resolve.
     * <p>
     * Resolve may be useful, for example, to filter out annotation changes that are not supposed to be copied across method hierarchy.
     * This method should be called only when {@link SuggestedRefactoringState#getErrorLevel()} == {@link SuggestedRefactoringState.ErrorLevel#NO_ERRORS}.
     */
    @RequiredReadAction
    public SuggestedRefactoringState refineSignaturesWithResolve(SuggestedRefactoringState state) {
        return state;
    }

    /**
     * Amends state with additional information by performing potentially slow computations such as usage search.
     * <p>
     * Normally, the state is amended by modifying {@link SuggestedRefactoringState#getAdditionalData()}
     * (see {@link SuggestedRefactoringState#withAdditionalData}). The additional data can be retrieved later and used by
     * implementations of {@link #detectAvailableRefactoring} and {@link #shouldSuppressRefactoringForDeclaration}.
     * This method should not be called if {@link SuggestedRefactoringState#getErrorLevel()} == {@link SuggestedRefactoringState.ErrorLevel#INCONSISTENT}.
     *
     * @return lazy iterator over sequence of amended states
     */
    @RequiredReadAction
    public Iterator<SuggestedRefactoringState> amendStateInBackground(SuggestedRefactoringState state) {
        return Collections.emptyIterator();
    }

    /**
     * Slow way to determine whether the action is available. Intended to be executed in background thread.
     */
    @RequiredReadAction
    public boolean isAvailable(SuggestedRefactoringState state) {
        return true;
    }

    /**
     * Determines refactoring availability for a given state and returns instance of {@link SuggestedRefactoringData},
     * providing information for presentation and execution of the refactoring.
     * <p>
     * It's supposed that this method is called only when <i>state.oldSignature != state.newSignature</i> and <i>state.syntaxError == false</i>.
     *
     * @param state current state of accumulated changes
     * @return An instance of {@link SuggestedRefactoringData} with information about available refactoring,
     * or <i>null</i> if no refactoring is available.
     */
    @RequiredReadAction
    public abstract @Nullable SuggestedRefactoringData detectAvailableRefactoring(SuggestedRefactoringState state);

    /**
     * Use this implementation of {@link SuggestedRefactoringAvailability}, if only Rename refactoring is supported for the language.
     */
    public static class RenameOnly extends SuggestedRefactoringAvailability {
        public RenameOnly(SuggestedRefactoringSupport refactoringSupport) {
            super(refactoringSupport);
        }

        @RequiredReadAction
        @Override
        public @Nullable SuggestedRefactoringData detectAvailableRefactoring(SuggestedRefactoringState state) {
            if (!(state.getAnchor() instanceof PsiNamedElement namedElement)) {
                return null;
            }
            return new SuggestedRenameData(namedElement, state.getOldSignature().getName());
        }
    }

    protected static boolean hasParameterAddedRemovedOrReordered(Signature oldSignature, Signature newSignature) {
        List<Parameter> oldParameters = oldSignature.getParameters();
        List<Parameter> newParameters = newSignature.getParameters();
        if (oldParameters.size() != newParameters.size()) {
            return true;
        }
        for (int i = 0; i < oldParameters.size(); i++) {
            if (!oldParameters.get(i).getId().equals(newParameters.get(i).getId())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasTypeChanges(Signature oldSignature, Signature newSignature) {
        List<Parameter> oldParameters = oldSignature.getParameters();
        List<Parameter> newParameters = newSignature.getParameters();
        if (oldParameters.size() != newParameters.size()) {
            throw new IllegalArgumentException("Signatures must have the same number of parameters");
        }
        if (!Objects.equals(oldSignature.getType(), newSignature.getType())) {
            return true;
        }

        for (int i = 0; i < oldParameters.size(); i++) {
            if (hasParameterTypeChanges(oldParameters.get(i), newParameters.get(i))) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasParameterTypeChanges(Parameter oldParam, Parameter newParam) {
        return !oldParam.getType().equals(newParam.getType());
    }

    @RequiredReadAction
    protected static Pair<Integer, @Nullable SuggestedRenameData> nameChanges(
        Signature oldSignature,
        Signature newSignature,
        PsiElement declaration,
        List<? extends PsiNamedElement> parameters
    ) {
        if (parameters.size() != newSignature.getParameters().size()) {
            throw new IllegalArgumentException("Parameters count must match new signature");
        }
        if (oldSignature.getParameters().size() != newSignature.getParameters().size()) {
            throw new IllegalArgumentException("Signatures must have the same number of parameters");
        }

        int nameChanges = 0;
        SuggestedRenameData renameData = null;

        if (declaration instanceof PsiNamedElement namedElement && !oldSignature.getName().equals(newSignature.getName())) {
            nameChanges++;
            renameData = new SuggestedRenameData(namedElement, oldSignature.getName());
        }

        for (int i = 0; i < oldSignature.getParameters().size(); i++) {
            Parameter oldParam = oldSignature.getParameters().get(i);
            Parameter newParam = newSignature.getParameters().get(i);
            if (!oldParam.getName().equals(newParam.getName())) {
                nameChanges++;
                renameData = new SuggestedRenameData(parameters.get(i), oldParam.getName());
            }
        }

        return Pair.create(nameChanges, nameChanges == 1 ? renameData : null);
    }
}
