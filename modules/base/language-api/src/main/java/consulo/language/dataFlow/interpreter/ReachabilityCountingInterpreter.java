// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.interpreter;

import consulo.language.dataFlow.interpreter.DfaListener;
import consulo.language.dataFlow.ir.ControlFlow;
import consulo.language.dataFlow.ir.DfaInstructionState;
import consulo.language.psi.PsiElement;

import java.util.BitSet;
import java.util.Set;

/**
 * An extended version of {@link StandardDataFlowInterpreter} which
 * tracks the instruction reachability.
 */
public class ReachabilityCountingInterpreter extends StandardDataFlowInterpreter {
  protected final BitSet myReached = new BitSet();

  /**
   * @param flow          control flow to interpret
   * @param listener      listener to use
   * @param stopOnNull    whether to stop interpretation on inevitable NullPointerException
   * @param stopOnCast    whether to stop interpretation on inevitable ClassCastException
   * @param startingIndex starting instruction index (usually, 0)
   */
  public ReachabilityCountingInterpreter(ControlFlow flow, DfaListener listener, boolean stopOnNull,
                                         boolean stopOnCast, int startingIndex) {
    super(flow, listener, stopOnNull, stopOnCast);
    myReached.set(0, startingIndex);
  }

  @Override
  protected DfaInstructionState [] acceptInstruction(DfaInstructionState instructionState) {
    myReached.set(instructionState.getInstruction().getIndex());
    return super.acceptInstruction(instructionState);
  }

  public Set<PsiElement> getUnreachable() {
    return myFlow.computeUnreachable(myReached);
  }
}
