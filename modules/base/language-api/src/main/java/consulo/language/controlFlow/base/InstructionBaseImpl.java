// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiElement;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class InstructionBaseImpl implements Instruction {

    private final List<Instruction> myPred = new SmartList<>();
    private final List<Instruction> mySucc = new SmartList<>();

    protected final @Nullable PsiElement myElement;

    @Override
    public @Nullable PsiElement getElement() {
        return myElement;
    }

    public InstructionBaseImpl(final @Nullable PsiElement element) {
        myElement = element;
    }

    @Override
    public final @Nonnull List<Instruction> allSucc() {
        return mySucc;
    }

    @Override
    public final @Nonnull List<Instruction> allPred() {
        return myPred;
    }

    @Override
    public @Nonnull String toString() {
        final StringBuilder builder = new StringBuilder(id());
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
    public @Nonnull String getElementPresentation() {
        return "element: " + myElement;
    }

    protected @Nonnull String id() {
        return String.valueOf(num());
    }
}
