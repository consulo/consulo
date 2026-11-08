// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.types.DfType;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import org.jspecify.annotations.Nullable;

/**
 * An instruction to eval unknown value (taking some operands from the stack)
 */
public class EvalUnknownInstruction extends EvalInstruction {
  private final DfType myResult;

  /**
   * @param anchor   PsiExpression to anchor to
   * @param operands number of operands
   */
  public EvalUnknownInstruction(@Nullable DfaAnchor anchor, int operands) {
    this(anchor, operands, DfType.TOP);
  }

  /**
   * @param anchor   PsiExpression to anchor to
   * @param operands number of operands
   * @param result result to push
   */
  public EvalUnknownInstruction(@Nullable DfaAnchor anchor, int operands, DfType result) {
    super(anchor, operands);
    myResult = result;
  }

  @Override
  public DfaValue eval(DfaValueFactory factory,
                                DfaMemoryState state,
                                DfaValue ... arguments) {
    return factory.fromDfType(myResult);
  }

  @Override
  public String toString() {
    return "EVAL_UNKNOWN " + getOperands();
  }
}
