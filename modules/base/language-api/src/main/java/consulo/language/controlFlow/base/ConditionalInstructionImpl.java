// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ConditionalInstruction;
import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ConditionalInstructionImpl extends InstructionImpl implements ConditionalInstruction {
    private final @Nullable PsiElement myCondition;
    private final boolean myResult;

    public ConditionalInstructionImpl(final @Nonnull ControlFlowBuilder builder,
                                      final @Nullable PsiElement element,
                                      final @Nullable PsiElement condition,
                                      final boolean result) {
        super(builder, element);
        myCondition = condition;
        myResult = result;
    }


    @Override
    public @Nullable PsiElement getCondition() {
        return myCondition;
    }

    @Override
    public boolean getResult() {
        return myResult;
    }

    @Override
    public @Nonnull String toString() {
        return super.toString() + ". Condition: " + (myCondition != null ? myCondition.getText() : null) + ":" + myResult;
    }
}
