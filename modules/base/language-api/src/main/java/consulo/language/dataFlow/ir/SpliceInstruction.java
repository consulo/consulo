// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Pop several elements from the stack and replace them with some of them (possibly duplicating, swapping, removing some, etc.)
 */
public class SpliceInstruction extends Instruction {
  private final int myCount;
  private final int[] myReplacement;

  public SpliceInstruction(int count, int... replacement) {
    myCount = count;
    myReplacement = replacement;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState stateBefore) {
    List<DfaValue> removed = IntStream.range(0, myCount).mapToObj(_ -> stateBefore.pop()).toList();
    Arrays.stream(myReplacement).mapToObj(removed::get).forEach(stateBefore::push);
    Instruction nextInstruction = interpreter.getInstruction(getIndex() + 1);
    return new DfaInstructionState[]{new DfaInstructionState(nextInstruction, stateBefore)};
  }

  @Override
  public String toString() {
    return "SPLICE [" + myCount + "] -> " + Arrays.toString(myReplacement);
  }
}
