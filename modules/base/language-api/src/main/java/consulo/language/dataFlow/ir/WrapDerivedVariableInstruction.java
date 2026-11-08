// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.ir.DfaAnchor;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.types.DfType;
import consulo.language.dataFlow.value.DerivedVariableDescriptor;
import consulo.language.dataFlow.value.DfaBinOpValue;
import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import org.jspecify.annotations.Nullable;

/**
 * Create a value that has given {@link DfType} and its {@link DerivedVariableDescriptor} value is taken from the stack.
 * In particular, could be used to box the stack value on JVM (with field = SpecialField.UNBOX).
 */
public class WrapDerivedVariableInstruction extends EvalInstruction {
  private final DfType myTargetType;
  private final DerivedVariableDescriptor myDerivedVariableDescriptor;

  /**
   * Construct an instruction without an anchor
   * 
   * @param targetType type of the target value
   * @param field descriptor of the field to wrap (the field value is expected to be on the stack)
   */
  public WrapDerivedVariableInstruction(DfType targetType, DerivedVariableDescriptor field) {
    this(targetType, field, null);
  }

  /**
   * Construct an instruction without an anchor
   *
   * @param targetType type of the target value
   * @param field descriptor of the field to wrap (the field value is expected to be on the stack)
   * @param anchor anchor to bind the instruction to
   */
  public WrapDerivedVariableInstruction(DfType targetType, DerivedVariableDescriptor field, @Nullable DfaAnchor anchor) {
    super(anchor, 1);
    myTargetType = targetType;
    myDerivedVariableDescriptor = field;
  }

  public DerivedVariableDescriptor getDerivedVariableDescriptor() {
    return myDerivedVariableDescriptor;
  }

  @Override
  public DfaValue eval(DfaValueFactory factory,
                                DfaMemoryState state,
                                DfaValue ... arguments) {
    DfaValue value = arguments[0];
    if (value instanceof DfaBinOpValue) {
      value = factory.fromDfType(state.getDfType(value));
    }
    return factory.getWrapperFactory().createWrapper(myTargetType, myDerivedVariableDescriptor, value);
  }

  @Override
  public String toString() {
    return "WRAP [" + myDerivedVariableDescriptor + "] " + myTargetType;
  }
}
