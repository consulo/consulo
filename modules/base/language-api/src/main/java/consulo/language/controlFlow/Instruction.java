// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.controlFlow;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface Instruction {

    Instruction[] EMPTY_ARRAY = new Instruction[0];

    /**
     * @return related psi elements. Can be null for fake instructions e.g. entry and exit points
     */
    @Nullable PsiElement getElement();

    /**
     * Outgoing edges
     */
    Collection<Instruction> allSucc();

    /**
     * Incoming edges
     */
    Collection<Instruction> allPred();

    int num();

    /**
     * element presentation is used in toString() for dumping the graph
     */
    String getElementPresentation();

    default void addSucc(Instruction endInstruction) {
        if (!allSucc().contains(endInstruction)) {
            allSucc().add(endInstruction);
        }
    }

    default void addPred(Instruction beginInstruction) {
        if (!allPred().contains(beginInstruction)) {
            allPred().add(beginInstruction);
        }
    }

    default void replacePred(Instruction oldInstruction, Collection<? extends Instruction> newInstructions) {
        newInstructions.forEach(el -> addPred(el));
        allPred().remove(oldInstruction);
    }

    default void replaceSucc(Instruction oldInstruction, Collection<? extends Instruction> newInstructions) {
        newInstructions.forEach(el -> addSucc(el));
        allSucc().remove(oldInstruction);
    }
}
