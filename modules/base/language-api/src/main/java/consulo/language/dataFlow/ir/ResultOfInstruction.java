// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;

import java.util.Objects;

/**
 * Instruction that does nothing but allows to attach an anchor to the top-of-stack
 */
public class ResultOfInstruction extends EvalInstruction {
  public ResultOfInstruction(DfaAnchor anchor) {
    super(anchor, 1);
  }

  @Override
  public DfaValue eval(DfaValueFactory factory, DfaMemoryState state, DfaValue ... arguments) {
    return arguments[0];
  }

  @Override
  public String toString() {
    return "RESULT_OF " + getDfaAnchor();
  }

  @Override
  public DfaAnchor getDfaAnchor() {
    return Objects.requireNonNull(super.getDfaAnchor());
  }
}
