// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class InstructionImpl extends InstructionBaseImpl {

    private final int myNumber;

    public InstructionImpl(final @Nonnull ControlFlowBuilder builder, final @Nullable PsiElement element) {
        super(element);
        myNumber = builder.instructionCount++;
    }

    @Override
    public final int num() {
        return myNumber;
    }
}