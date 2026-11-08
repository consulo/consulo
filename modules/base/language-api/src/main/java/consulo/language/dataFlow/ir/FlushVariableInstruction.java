// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.DfaVariableValue;

/**
 * Flush single variable
 */
public class FlushVariableInstruction extends Instruction {
  private final DfaVariableValue myVariable;

  /**
   * @param variable variable to flush
   */
  public FlushVariableInstruction(DfaVariableValue variable) {
    myVariable = variable;
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    return new FlushVariableInstruction(myVariable.bindToFactory(factory));
  }

  public DfaVariableValue getVariable() {
    return myVariable;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    stateBefore.flushVariable(getVariable());
    return nextStates(interpreter, stateBefore);
  }

  @Override
  public String toString() {
    return "FLUSH " + myVariable;
  }
}
