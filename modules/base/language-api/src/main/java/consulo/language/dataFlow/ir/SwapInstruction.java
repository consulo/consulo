// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;

/**
 * Swaps two top-of-stack values
 */
public class SwapInstruction extends Instruction {

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    final DfaValue a = stateBefore.pop();
    final DfaValue b = stateBefore.pop();
    stateBefore.push(a);
    stateBefore.push(b);
    return nextStates(interpreter, stateBefore);
  }

  @Override
  public String toString() {
    return "SWAP";
  }
}
