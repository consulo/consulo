// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaControlTransferValue;
import consulo.language.dataFlow.value.DfaValueFactory;

import java.util.Arrays;

/**
 * Instruction which performs complex control transfer (handling exception; processing finally blocks; exiting inlined lambda, etc.)
 */
public class ControlTransferInstruction extends Instruction {
  private final DfaControlTransferValue myTransfer;

  public ControlTransferInstruction(DfaControlTransferValue transfer) {
    this(transfer, true);
  }

  public ControlTransferInstruction(DfaControlTransferValue transfer, boolean linkTraps) {
    myTransfer = transfer;
    if (linkTraps) {
      transfer.getTraps().forEach(trap -> trap.link(transfer));
    }
  }

  public DfaControlTransferValue getTransfer() {
    return myTransfer;
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    return new ControlTransferInstruction(myTransfer.bindToFactory(factory), false);
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter,
                                      DfaMemoryState stateBefore) {
    return myTransfer.dispatch(stateBefore, interpreter).toArray(DfaInstructionState.EMPTY_ARRAY);
  }

  /**
   * Returns list of possible target instruction indices
   */
  @Override
  public int [] getSuccessorIndexes() {
    return myTransfer.getPossibleTargetIndices();
  }

  @Override
  public boolean isLinear() {
    return false;
  }

  @Override
  public String toString() {
    int[] indexes = getSuccessorIndexes();
    return "TRANSFER " + myTransfer + (indexes.length == 0 ? "" : " [targets: " + Arrays.toString(indexes) + "]");
  }
}
