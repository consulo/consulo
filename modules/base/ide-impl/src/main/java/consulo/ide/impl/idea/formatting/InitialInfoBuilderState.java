// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.formatting;

import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.internal.AbstractBlockWrapper;
import consulo.language.codeStyle.internal.CompositeBlockWrapper;
import consulo.language.codeStyle.internal.IndentImpl;
import consulo.language.codeStyle.internal.WrapImpl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class InitialInfoBuilderState {
  public final Block parentBlock;
  public final WrapImpl parentBlockWrap;
  public final CompositeBlockWrapper wrappedBlock;

  public Block previousBlock;

  private final List<AbstractBlockWrapper> myWrappedChildren = new ArrayList<>();

  InitialInfoBuilderState(@Nonnull Block parentBlock, @Nonnull CompositeBlockWrapper wrappedBlock, @Nullable WrapImpl parentBlockWrap) {
    this.parentBlock = parentBlock;
    this.wrappedBlock = wrappedBlock;
    this.parentBlockWrap = parentBlockWrap;
  }

  public int getIndexOfChildBlockToProcess() {
    return myWrappedChildren.size();
  }

  public boolean childBlockProcessed(@Nonnull Block child, @Nonnull AbstractBlockWrapper wrappedChild, CommonCodeStyleSettings.IndentOptions options) {
    myWrappedChildren.add(wrappedChild);
    previousBlock = child;

    int subBlocksNumber = parentBlock.getSubBlocks().size();
    if (myWrappedChildren.size() > subBlocksNumber) {
      return true;
    }
    else if (myWrappedChildren.size() == subBlocksNumber) {
      setDefaultIndents(myWrappedChildren, options.USE_RELATIVE_INDENTS);
      wrappedBlock.setChildren(myWrappedChildren);
      return true;
    }
    return false;
  }

  public boolean isProcessed() {
    return myWrappedChildren.size() == parentBlock.getSubBlocks().size();
  }

  private static void setDefaultIndents(final List<AbstractBlockWrapper> list, boolean useRelativeIndents) {
    for (AbstractBlockWrapper wrapper : list) {
      if (wrapper.getIndent() == null) {
        wrapper.setIndent((IndentImpl)Indent.getContinuationWithoutFirstIndent(useRelativeIndents));
      }
    }
  }

}