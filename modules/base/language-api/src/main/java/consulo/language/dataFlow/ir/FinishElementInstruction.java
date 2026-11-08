// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.interpreter.DataFlowInterpreter;
import consulo.language.dataFlow.memory.DfaMemoryState;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.VariableDescriptor;
import consulo.language.psi.PsiElement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FinishElementInstruction extends Instruction {
  private final Set<VariableDescriptor> myVarsToFlush = new HashSet<>();
  private final PsiElement myElement;

  public FinishElementInstruction(PsiElement element) {
    myElement = element;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowInterpreter interpreter, DfaMemoryState state) {
    if (!myVarsToFlush.isEmpty()) {
      state.forgetVariables(var -> myVarsToFlush.contains(var.getDescriptor()));
    }
    return nextStates(interpreter, state);
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    if (myVarsToFlush.isEmpty()) return this;
    // Derived analysis may change myVarsToFlush
    // E.g. see consulo.language.dataFlow.DfaPsiUtil#getBlockNotNullFields
    // (bad idea, but this is how it's done now)
    // So we still need to copy instruction to detach myVarsToFlush list
    var instruction = new FinishElementInstruction(myElement);
    instruction.flushVars(myVarsToFlush);
    return instruction;
  }

  /**
   * Add variables with given descriptors to the flush list
   *
   * @param vars vars to flush
   */
  public void flushVars(Collection<VariableDescriptor> vars) {
    myVarsToFlush.addAll(vars);
  }

  /**
   * Removes variable descriptors from the flush list that match the given predicate.
   *
   * @param predicate the predicate used to determine which variable descriptors to remove
   */
  public void removeFromFlushList(Predicate<? super VariableDescriptor> predicate) {
    myVarsToFlush.removeIf(predicate);
  }

  /**
   * @return true if this instruction may flush some variables
   */
  public boolean mayFlushSomething() {
    return !myVarsToFlush.isEmpty();
  }

  @Override
  public String toString() {
    return "FINISH " + (myElement == null ? "" : myElement) + (myVarsToFlush.isEmpty() ? "" : "; flushing " + myVarsToFlush);
  }
}
