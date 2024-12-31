// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.controlFlow;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;

public interface Instruction {

    Instruction[] EMPTY_ARRAY = new Instruction[0];

    /**
     * @return related psi elements. Can be null for fake instructions e.g. entry and exit points
     */
    @Nullable
    PsiElement getElement();

    /**
     * Outgoing edges
     */
    @Nonnull
    Collection<Instruction> allSucc();

    /**
     * Incoming edges
     */
    @Nonnull
    Collection<Instruction> allPred();

    int num();

    /**
     * element presentation is used in toString() for dumping the graph
     */
    @Nonnull
    @NonNls
    String getElementPresentation();

    default void addSucc(@Nonnull Instruction endInstruction) {
        if (!allSucc().contains(endInstruction)) {
            allSucc().add(endInstruction);
        }
    }

    default void addPred(@Nonnull Instruction beginInstruction) {
        if (!allPred().contains(beginInstruction)) {
            allPred().add(beginInstruction);
        }
    }

    default void replacePred(@Nonnull Instruction oldInstruction, @Nonnull Collection<? extends Instruction> newInstructions) {
        newInstructions.forEach(el -> addPred(el));
        allPred().remove(oldInstruction);
    }

    default void replaceSucc(@Nonnull Instruction oldInstruction, @Nonnull Collection<? extends Instruction> newInstructions) {
        newInstructions.forEach(el -> addSucc(el));
        allSucc().remove(oldInstruction);
    }
}
