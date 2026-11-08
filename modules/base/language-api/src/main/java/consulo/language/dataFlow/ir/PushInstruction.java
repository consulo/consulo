// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.DfaVariableValue;
import consulo.language.dataFlow.value.VariableDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An instruction that pushes given value to the stack
 */
public class PushInstruction extends EvalInstruction {
  private final DfaValue myValue;

  public PushInstruction(DfaValue value, DfaAnchor place) {
    super(place, 0);
    myValue = value;
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    return new PushInstruction(getValue().bindToFactory(factory), getDfaAnchor());
  }

  @Override
  public List<VariableDescriptor> getRequiredDescriptors(DfaValueFactory factory) {
    if (getValue() instanceof DfaVariableValue var) {
      return Stream.iterate(var, Objects::nonNull, DfaVariableValue::getQualifier)
        .map(DfaVariableValue::getDescriptor)
        .toList();
    }
    return List.of();
  }

  public DfaValue getValue() {
    return myValue;
  }

  @Override
  public DfaValue eval(DfaValueFactory factory, DfaMemoryState state, DfaValue ... arguments) {
    return myValue;
  }

  @Override
  public String toString() {
    return "PUSH " + myValue;
  }
}
