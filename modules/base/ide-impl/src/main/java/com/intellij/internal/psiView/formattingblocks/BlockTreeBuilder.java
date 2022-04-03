package com.intellij.internal.psiView.formattingblocks;

import consulo.ui.ex.tree.IndexComparator;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class BlockTreeBuilder extends SimpleTreeBuilder {

  public BlockTreeBuilder(JTree tree, BlockTreeStructure blockTreeStructure) {
    super(tree, new DefaultTreeModel(new DefaultMutableTreeNode()), blockTreeStructure, IndexComparator.INSTANCE);
    initRootNode();
  }
}
