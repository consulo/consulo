package consulo.ide.impl.idea.internal.psiView.formattingblocks;

import consulo.ui.ex.awt.tree.SimpleTreeStructure;

public class BlockTreeStructure extends SimpleTreeStructure {
  private BlockTreeNode myRoot;

  @Override
  public BlockTreeNode getRootElement() {
    return myRoot;
  }

  public void setRoot(BlockTreeNode root) {
    myRoot = root;
  }
}
