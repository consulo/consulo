// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;

import java.util.Objects;

/**
 * An (instruction, memoryState) pair that represents single possible abstract interpreter state.
 */
public class DfaInstructionState implements Comparable<DfaInstructionState> {
  public static final DfaInstructionState[] EMPTY_ARRAY = new DfaInstructionState[0];
  private final DfaMemoryState myBeforeMemoryState;
  private final Instruction myInstruction;

  public DfaInstructionState(Instruction myInstruction, DfaMemoryState myBeforeMemoryState) {
    this.myBeforeMemoryState = myBeforeMemoryState;
    this.myInstruction = myInstruction;
  }

  public Instruction getInstruction() {
    return myInstruction;
  }

  /**
   * A helper method to create the resulting states for linear instruction
   * 
   * @param interpreter interpreter that interprets current IR
   * @return an array of single instruction state containing the next instruction. 
   */
  public DfaInstructionState[] nextStates(DataFlowInterpreter interpreter) {
    return myInstruction.nextStates(interpreter, myBeforeMemoryState);
  }

  public DfaMemoryState getMemoryState() {
    return myBeforeMemoryState;
  }

  @Override
  public String toString() {
    return getInstruction().getIndex() + " " + getInstruction() + ":   " + getMemoryState();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DfaInstructionState state = (DfaInstructionState)o;
    return Objects.equals(myBeforeMemoryState, state.myBeforeMemoryState) &&
           Objects.equals(myInstruction, state.myInstruction);
  }

  @Override
  public int hashCode() {
    return 31 * myBeforeMemoryState.hashCode() + myInstruction.hashCode();
  }

  @Override
  public int compareTo(DfaInstructionState o) {
    return Integer.compare(myInstruction.getIndex(), o.myInstruction.getIndex());
  }
}