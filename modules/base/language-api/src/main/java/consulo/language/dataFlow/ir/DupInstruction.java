// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;

/**
 * Duplicates top-of-stack value
 */
public class DupInstruction extends Instruction {
  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState memState) {
    memState.push(memState.peek());
    return nextStates(interpreter, memState);
  }

  @Override
  public String toString() {
    return "DUP";
  }
}
