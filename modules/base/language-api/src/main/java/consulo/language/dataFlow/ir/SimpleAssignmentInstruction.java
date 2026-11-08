// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.DfaVariableValue;
import org.jspecify.annotations.Nullable;

/**
 * Assign the value from the stack to a specified static destination
 */
public class SimpleAssignmentInstruction extends ExpressionPushingInstruction {
  private final DfaVariableValue myDestination;

  public SimpleAssignmentInstruction(@Nullable DfaAnchor anchor, DfaVariableValue destination) {
    super(anchor);
    myDestination = destination;
  }

  public DfaVariableValue getDestination() {
    return myDestination;
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    return new SimpleAssignmentInstruction(getDfaAnchor(), myDestination.bindToFactory(factory));
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter,
                                      DfaMemoryState stateBefore) {
    DfaValue value = stateBefore.pop();
    interpreter.getListener().beforeAssignment(value, myDestination, stateBefore, getDfaAnchor());
    stateBefore.setVarValue(myDestination, value);
    interpreter.getListener().afterAssignment(value, myDestination, stateBefore, getDfaAnchor());
    pushResult(interpreter, stateBefore, myDestination);
    return nextStates(interpreter, stateBefore);
  }

  @Override
  public String toString() {
    return "ASSIGN_TO " + myDestination;
  }
}
