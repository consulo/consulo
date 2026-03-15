// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.TransparentInstruction;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public class TransparentInstructionImpl extends InstructionBaseImpl implements TransparentInstruction {

    private final String myMarkerName;
    private final int myNum;

    public TransparentInstructionImpl(ControlFlowBuilder builder,
                                      @Nullable PsiElement element,
                                      String markerName) {
        super(element);
        myMarkerName = markerName;
        myNum = builder.transparentInstructionCount++;
    }

    @Override
    public String getElementPresentation() {
        return super.getElementPresentation() + "(" + myMarkerName + ")";
    }

    @Override
    protected String id() {
        return "t" + num();
    }

    @Override
    public int num() {
        return myNum;
    }
}
