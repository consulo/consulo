package com.intellij.coverage.view;

import com.intellij.ide.util.treeView.AlphaComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.ColumnInfo;

import java.util.Comparator;

/**
* User: anna
* Date: 1/9/12
*/
public class ElementColumnInfo extends ColumnInfo<NodeDescriptor, String> {
  public ElementColumnInfo() {
    super("Element");
  }

  @Override
  public Comparator<NodeDescriptor> getComparator() {
    return AlphaComparator.INSTANCE;
  }

  @Override
  public String valueOf(NodeDescriptor node) {
    return node.toString();
  }
}
