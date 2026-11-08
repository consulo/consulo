// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.value.DfaControlTransferValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.psi.PsiElement;
import consulo.util.collection.FList;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * An instruction to exit the interpretation (but possibly process traps like 'finally' blocks).
 */
public class ReturnInstruction extends ControlTransferInstruction {
  private final PsiElement myAnchor;

  public ReturnInstruction(DfaValueFactory factory, FList<DfaControlTransferValue.Trap> traps, @Nullable PsiElement anchor) {
    this(factory.controlTransfer(DfaControlTransferValue.RETURN_TRANSFER, traps), anchor, true);
  }

  private ReturnInstruction(DfaControlTransferValue transfer, @Nullable PsiElement anchor, boolean linkTraps) {
    super(transfer, linkTraps);
    myAnchor = anchor;
  }

  @Override
  public Instruction bindToFactory(DfaValueFactory factory) {
    return new ReturnInstruction(getTransfer().bindToFactory(factory), myAnchor, false);
  }

  public @Nullable PsiElement getAnchor() {
    return myAnchor;
  }

  @Override
  public String toString() {
    int[] indexes = getSuccessorIndexes();
    return "RETURN" + (indexes.length == 0 ? "" : " [traps: " + Arrays.toString(indexes) + "]");
  }
}
