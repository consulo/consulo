/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide;

import com.intellij.util.ui.tree.TreeUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author yole
 */
public class DefaultTreeExpander implements TreeExpander {
  private final Supplier<? extends JTree> myTreeSupplier;

  public DefaultTreeExpander(@Nonnull JTree tree) {
    this(() -> tree);
  }

  public DefaultTreeExpander(@Nonnull Supplier<? extends JTree> treeSupplier) {
    myTreeSupplier = treeSupplier;
  }

  @Override
  public void expandAll() {
    JTree tree = myTreeSupplier.get();
    TreeUtil.expandAll(tree);
    showSelectionCentered(tree);
  }

  @Override
  public boolean canExpand() {
    return myTreeSupplier.get().isShowing();
  }

  @Override
  public void collapseAll() {
    JTree tree = myTreeSupplier.get();

    collapseAll(tree, 1);
  }

  protected void collapseAll(JTree tree, int keepSelectionLevel) {
    collapseAll(tree, true, keepSelectionLevel);
  }

  protected void collapseAll(JTree tree, boolean strict, int keepSelectionLevel) {
    TreeUtil.collapseAll(tree, strict, keepSelectionLevel);
    showSelectionCentered(tree);
  }

  private void showSelectionCentered(JTree tree) {
    final int[] rowz = tree.getSelectionRows();
    if (rowz != null && rowz.length > 0) {
      TreeUtil.showRowCentered(tree, rowz[0], false);
    }
  }

  @Override
  public boolean canCollapse() {
    return myTreeSupplier.get().isShowing();
  }
}
