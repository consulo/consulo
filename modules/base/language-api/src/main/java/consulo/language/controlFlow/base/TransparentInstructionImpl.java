// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.TransparentInstruction;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TransparentInstructionImpl extends InstructionBaseImpl implements TransparentInstruction {

    private final @Nonnull String myMarkerName;
    private final int myNum;

    public TransparentInstructionImpl(final @Nonnull ControlFlowBuilder builder,
                                      final @Nullable PsiElement element,
                                      @Nonnull String markerName) {
        super(element);
        myMarkerName = markerName;
        myNum = builder.transparentInstructionCount++;
    }

    @Override
    public @Nonnull String getElementPresentation() {
        return super.getElementPresentation() + "(" + myMarkerName + ")";
    }

    @Override
    protected @Nonnull String id() {
        return "t" + num();
    }

    @Override
    public int num() {
        return myNum;
    }
}
