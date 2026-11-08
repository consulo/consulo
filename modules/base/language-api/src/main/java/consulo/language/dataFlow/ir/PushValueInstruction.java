// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.dataFlow.ir;

import org.jspecify.annotations.Nullable;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.types.DfType;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;

/**
 * An instruction that pushes the value of given DfType to the stack
 */
public class PushValueInstruction extends EvalInstruction {
  private final DfType myValue;

  public PushValueInstruction(DfType value, @Nullable DfaAnchor place) {
    super(place, 0);
    myValue = value;
  }

  public PushValueInstruction(DfType value) {
    this(value, null);
  }

  public DfType getValue() {
    return myValue;
  }

  @Override
  public DfaValue eval(DfaValueFactory factory, DfaMemoryState state, DfaValue ... arguments) {
    return factory.fromDfType(myValue);
  }

  @Override
  public String toString() {
    return "PUSH_VAL " + myValue;
  }
}
