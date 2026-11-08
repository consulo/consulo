// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.VariableDescriptor;

import java.util.Collections;
import java.util.List;

/**
 * A base class for all instructions of DFA IR that could be interpreted.
 * There are language agnostic instructions as well as language specific ones.
 * Every instruction knows how to interpret itself.
 */
public abstract class Instruction {
  private int myIndex = -1;

  /**
   * Interpret the instruction and return the resulting instruction states
   *
   * @param interpreter interpreter that interprets current IR
   * @param stateBefore input memory state. Could be modified during the interpretation
   * @return an array of output memory states. Non-branching instructions return an array of one element.
   * Returning an empty array means that the interpretation stops here 
   * (e.g. 'return' instruction or throwing an exception).
   */
  public abstract DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore);

  /**
   * Set the current instruction index within the IR it belongs to. Can be done only once.
   * @param index instruction index to set.
   */
  public void setIndex(int index) {
    if (myIndex != -1) {
      throw new IllegalStateException("Index is already set: old = " + myIndex + "; new = " + index);
    }
    myIndex = index;
  }

  /**
   * @return the instruction index within the IR it belongs to.
   */
  public int getIndex() {
    return myIndex;
  }

  /**
   * @return array of possible next instruction indexes that could be executed after this one.
   * Default implementation returns the next index which is suitable for linear instructions.
   */
  public int [] getSuccessorIndexes() {
    return new int[] {myIndex + 1};
  }

  /**
   * @return list of variable descriptors that must stay reachable at this instruction. 
   * The variables that are not required to be reachable by any instructions 
   * until the end of the interpretation could be flushed automatically.
   */
  public List<VariableDescriptor> getRequiredDescriptors(DfaValueFactory factory) {
    return Collections.emptyList();
  }
  
  /**
   * @return true if the instruction always moves to the next instruction. Note that the linear instruction
   * may still split the memory state returning several instruction states from accept (but all of them
   * point to the next instruction).
   */
  public boolean isLinear() {
    int[] indexes = getSuccessorIndexes();
    return indexes.length == 1 && indexes[0] == myIndex + 1;
  }

  /**
   * Copy instruction to another factory. May return itself if the instruction does not depend on the factory.
   *
   * @param factory factory to place the instruction
   * @return copy that bound to the supplied factory, or itself if the instruction does not depend on the factory.
   */
  public Instruction bindToFactory(DfaValueFactory factory) {
    return this;
  }

  /**
   * A helper method to create the resulting states for linear instruction 
   * @param interpreter interpreter for current IR
   * @param memState resulting memory state
   * @return an array of a single instruction state that refers to the next instruction and the supplied memory state
   */
  protected final DfaInstructionState [] nextStates(DataFlowInterpreter interpreter, DfaMemoryState memState) {
    return new DfaInstructionState[]{nextState(interpreter, memState)};
  }

  /**
   * A helper method to create the resulting state pointing to the next instruction 
   * @param interpreter interpreter for current IR
   * @param memState resulting memory state
   * @return an instruction state that refers to the next instruction and the supplied memory state
   */
  protected final DfaInstructionState nextState(DataFlowInterpreter interpreter, DfaMemoryState memState) {
    return new DfaInstructionState(interpreter.getInstruction(getIndex() + 1), memState);
  }
}
