// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.VariableDescriptor;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Instruction to push a field qualified by the value on the stack
 */
public class GetQualifiedValueInstruction extends EvalInstruction {
  private final VariableDescriptor myDescriptor;

  public GetQualifiedValueInstruction(VariableDescriptor descriptor) {
    this(descriptor, null);
  }

  public GetQualifiedValueInstruction(VariableDescriptor descriptor, @Nullable DfaAnchor anchor) {
    super(anchor, 1);
    myDescriptor = descriptor;
  }

  @Override
  public DfaValue eval(DfaValueFactory factory, DfaMemoryState state, DfaValue ... arguments) {
    return myDescriptor.createValue(factory, arguments[0]);
  }

  @Override
  public List<VariableDescriptor> getRequiredDescriptors(DfaValueFactory factory) {
    return List.of(myDescriptor);
  }

  @Override
  public String toString() {
    return "GET_FIELD " + myDescriptor;
  }
}
