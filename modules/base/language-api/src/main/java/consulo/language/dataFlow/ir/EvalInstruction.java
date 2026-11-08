// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import org.jspecify.annotations.Nullable;

/**
 * An instruction that takes fixed number of operands from the stack and computes a single result without branching.
 * Assumed to be always successful (without exception branches).
 */
public abstract class EvalInstruction extends ExpressionPushingInstruction {
  private final int myOperands;

  /**
   * @param anchor PsiExpression to anchor to
   * @param operands number of operands
   */
  protected EvalInstruction(@Nullable DfaAnchor anchor, int operands) {
    super(anchor);
    myOperands = operands;
  }

  /**
   * @return number of operands
   */
  public final int getOperands() {
    return myOperands;
  }

  @Override
  public final DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    int operands = getOperands();
    DfaValue[] args;
    if (operands == 0) {
      args = DfaValue.EMPTY_ARRAY;
    } else {
      args = new DfaValue[operands];
      for (int i = operands - 1; i >= 0; i--) {
        args[i] = stateBefore.pop();
      }
    }
    DfaValue value = eval(interpreter.getFactory(), stateBefore, args);
    pushResult(interpreter, stateBefore, value, args);
    return nextStates(interpreter, stateBefore);
  }

  /**
   * @param factory value factory to use to produce new values
   * @param state state. Must not be modified, could be used to query information only
   * @param arguments operation arguments (length must be the same as {@link #getOperands()} result)
   * @return the evaluated value
   */
  public abstract DfaValue eval(DfaValueFactory factory,
                                         DfaMemoryState state,
                                         DfaValue ... arguments);
}
