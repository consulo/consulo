// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A service performing actual changes in the code when user executes suggested refactoring.
 */
public abstract class SuggestedRefactoringExecution {
    protected final SuggestedRefactoringSupport refactoringSupport;

    protected SuggestedRefactoringExecution(SuggestedRefactoringSupport refactoringSupport) {
        this.refactoringSupport = refactoringSupport;
    }

    /**
     * Prepares data for Change Signature refactoring while the declaration has state with user changes (the new signature).
     */
    public abstract @Nullable Object prepareChangeSignature(SuggestedChangeSignatureData data);

    /**
     * Performs Change Signature refactoring. This method is invoked with the declaration reverted to its original state
     * before user changes (the old signature). The list of imports is also reverted to the original state.
     */
    public abstract void performChangeSignature(
        SuggestedChangeSignatureData data,
        List<NewParameterValue> newParameterValues,
        @Nullable Object preparedData
    );

    /**
     * Use this implementation of {@link SuggestedRefactoringExecution}, if only Rename refactoring is supported for the language.
     */
    public static class RenameOnly extends SuggestedRefactoringExecution {
        public RenameOnly(SuggestedRefactoringSupport refactoringSupport) {
            super(refactoringSupport);
        }

        @Override
        public void performChangeSignature(
            SuggestedChangeSignatureData data,
            List<NewParameterValue> newParameterValues,
            @Nullable Object preparedData
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Object prepareChangeSignature(SuggestedChangeSignatureData data) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Class representing value for a new parameter to be used for updating arguments of calls.
     */
    public sealed interface NewParameterValue {
        None NONE = new None();
        AnyVariable ANY_VARIABLE = new AnyVariable();

        final class None implements NewParameterValue {
            private None() {
            }
        }

        record Expression(PsiElement expression) implements NewParameterValue {
        }

        final class AnyVariable implements NewParameterValue {
            private AnyVariable() {
            }
        }
    }
}
