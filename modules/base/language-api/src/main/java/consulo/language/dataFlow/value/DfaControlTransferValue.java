// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.value;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.ir.DfaInstructionState;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.FList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Collections;
import java.util.List;

/**
 * A value that could be pushed to the stack and used for control transfer
 */
public final class DfaControlTransferValue extends DfaValue {
  private final TransferTarget target;
  private final FList<Trap> traps;

  DfaControlTransferValue(DfaValueFactory factory,
                          TransferTarget target,
                          FList<Trap> traps) {
    super(factory);
    this.traps = traps;
    this.target = target;
  }

  @Override
  public String toString() {
    return target + (traps.isEmpty() ? "" : " " + traps);
  }

  @Override
  public DfaControlTransferValue bindToFactory(DfaValueFactory factory) {
    return factory.controlTransfer(target.bindToFactory(factory), traps);
  }

  public TransferTarget getTarget() {
    return target;
  }

  public FList<Trap> getTraps() {
    return traps;
  }

  public int [] getPossibleTargetIndices() {
    IntOpenHashSet indices = new IntOpenHashSet();
    for (Trap trap : traps) {
      for (int possibleTarget : trap.getPossibleTargets()) {
        indices.add(possibleTarget);
      }
    }
    for (int possibleTarget : target.getPossibleTargets()) {
      indices.add(possibleTarget);
    }
    return indices.toIntArray();
  }

  public List<DfaInstructionState> dispatch(DfaMemoryState state, DataFlowInterpreter interpreter) {
    return dispatch(state, interpreter, target, traps);
  }

  public static List<DfaInstructionState> dispatch(DfaMemoryState state,
                                                                          DataFlowInterpreter interpreter,
                                                                          TransferTarget target,
                                                                          FList<Trap> nextTraps) {
    Trap head = nextTraps.getHead();
    nextTraps = nextTraps.getTail() == null ? FList.emptyList() : nextTraps.getTail();
    state.emptyStack();
    if (head != null) {
      return head.dispatch(state, interpreter, target, nextTraps);
    }
    return target.dispatch(state, interpreter);
  }


  /**
   * Represents the target location.
   */
  public interface TransferTarget {
    /**
     * @return array of possible instruction offsets for given target
     */
    default int [] getPossibleTargets() {
      return ArrayUtil.EMPTY_INT_ARRAY;
    }

    /** 
     * @return next instruction states assuming no traps 
     */
    default List<DfaInstructionState> dispatch(DfaMemoryState state, DataFlowInterpreter interpreter) {
      return Collections.emptyList();
    }

    /**
     * @param factory to bind this target to
     * @return transfer target bound to another factory. By default, its assumed that TransferTarget is factory-agnostic
     */
    default TransferTarget bindToFactory(DfaValueFactory factory) {
      return this;
    }
  }

  /**
   * A transfer that returns from the current scope
   */
  public static final TransferTarget RETURN_TRANSFER = new TransferTarget() {
    @Override
    public String toString() {
      return "Return";
    }
  };

  /**
   * Represents traps (e.g. catch sections) that may prevent normal transfer
   */
  public interface Trap {
    /**
     * @return array of possible instruction offsets for given trap
     */
    default int [] getPossibleTargets() {
      return ArrayUtil.EMPTY_INT_ARRAY;
    }

    default void link(DfaControlTransferValue value) {
    }

    List<DfaInstructionState> dispatch(DfaMemoryState state,
                                                DataFlowInterpreter interpreter,
                                                TransferTarget target,
                                                FList<Trap> nextTraps);

    /**
     * @return PSI anchor (e.g. catch section)
     */
    PsiElement getAnchor();
  }
}
