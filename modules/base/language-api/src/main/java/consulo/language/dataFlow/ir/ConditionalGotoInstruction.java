// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.ir;


import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.types.DfType;
import consulo.language.dataFlow.value.DfaCondition;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/**
 * Conditionally jump if the value on stack top is equal to the specified value (top-of-stack value is being popped)
 */
public class ConditionalGotoInstruction extends Instruction {
  private ControlFlow.ControlFlowOffset myOffset;
  private final DfType myCompareTo;
  private final PsiElement myAnchor;

  /**
   * @param offset target offset to jump to
   * @param compareTo value to compare to
   */
  public ConditionalGotoInstruction(ControlFlow.ControlFlowOffset offset, DfType compareTo) {
    this(offset, compareTo, null);
  }
  
  public ConditionalGotoInstruction(ControlFlow.ControlFlowOffset offset, DfType compareTo, @Nullable PsiElement psiAnchor) {
    myAnchor = psiAnchor;
    myOffset = offset;
    myCompareTo = compareTo;
  }

  /**
   * @return PSI element associated with this instruction
   * @deprecated used in "Find the cause" feature only. Will be removed
   */
  @Deprecated(forRemoval = true)
  public @Nullable PsiElement getPsiAnchor() {
    return myAnchor;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    DfaCondition condTrue = stateBefore.pop().eq(myCompareTo);
    DfaCondition condFalse = condTrue.negate();

    if (condTrue == DfaCondition.getTrue()) {
      return new DfaInstructionState[] {new DfaInstructionState(interpreter.getInstruction(getOffset()), stateBefore)};
    }

    if (condFalse == DfaCondition.getTrue()) {
      return nextStates(interpreter, stateBefore);
    }

    ArrayList<DfaInstructionState> result = new ArrayList<>(2);

    DfaMemoryState elseState = stateBefore.createCopy();

    if (stateBefore.applyCondition(condTrue)) {
      result.add(new DfaInstructionState(interpreter.getInstruction(getOffset()), stateBefore));
    }

    if (elseState.applyCondition(condFalse)) {
      result.add(nextState(interpreter, elseState));
    }

    return result.toArray(DfaInstructionState.EMPTY_ARRAY);
  }

  @Override
  public int [] getSuccessorIndexes() {
    int offset = getOffset();
    return offset == getIndex() + 1 ? new int[] {offset} : new int[] {getIndex() + 1, offset};
  }

  @Override
  public String toString() {
    return "IF_EQ " + myCompareTo + " " + myOffset;
  }

  public boolean isTarget(DfType valueOnStack, Instruction target) {
    return target.getIndex() == (valueOnStack.equals(myCompareTo) ? getOffset() : getIndex() + 1);
  }

  public int getOffset() {
    return myOffset.getInstructionOffset();
  }

  public void setOffset(final int offset) {
    myOffset = new ControlFlow.FixedOffset(offset);
  }
}
