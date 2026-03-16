// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public class InstructionImpl extends InstructionBaseImpl {

    private final int myNumber;

    public InstructionImpl(ControlFlowBuilder builder, @Nullable PsiElement element) {
        super(element);
        myNumber = builder.instructionCount++;
    }

    @Override
    public final int num() {
        return myNumber;
    }
}