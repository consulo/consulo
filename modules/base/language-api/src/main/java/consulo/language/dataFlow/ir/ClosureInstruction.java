// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * Indicates the closures inside the analyzed code block
 */
public class ClosureInstruction extends Instruction {
  private final List<PsiElement> myClosures;

  public ClosureInstruction(List<PsiElement> closures) {
    myClosures = closures;
  }

  public List<PsiElement> getClosureElements() {
    return myClosures;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    for (PsiElement element : getClosureElements()) {
      interpreter.createClosureState(element, stateBefore);
    }
    return nextStates(interpreter, stateBefore);
  }

  @Override
  public String toString() {
    return "CLOSURE";
  }
}
