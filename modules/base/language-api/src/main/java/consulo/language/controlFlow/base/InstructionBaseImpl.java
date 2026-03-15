// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiElement;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class InstructionBaseImpl implements Instruction {

    private final List<Instruction> myPred = new SmartList<>();
    private final List<Instruction> mySucc = new SmartList<>();

    protected final @Nullable PsiElement myElement;

    @Override
    public @Nullable PsiElement getElement() {
        return myElement;
    }

    public InstructionBaseImpl(@Nullable PsiElement element) {
        myElement = element;
    }

    @Override
    public final List<Instruction> allSucc() {
        return mySucc;
    }

    @Override
    public final List<Instruction> allPred() {
        return myPred;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(id());
        builder.append("(");
        for (int i = 0; i < mySucc.size(); i++) {
            if (i > 0) builder.append(',');
            Instruction instruction = mySucc.get(i);
            int num = instruction.num();
            if (instruction instanceof InstructionBaseImpl) {
                builder.append(((InstructionBaseImpl) instruction).id());
            }
            else {
                builder.append(num);
            }
        }
        builder.append(") ").append(getElementPresentation());
        return builder.toString();
    }

    @Override
    public String getElementPresentation() {
        return "element: " + myElement;
    }

    protected String id() {
        return String.valueOf(num());
    }
}
