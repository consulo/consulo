/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtTreeImpl<E> extends SWTComponentDelegate<org.eclipse.swt.widgets.Tree> implements Tree<E> {
  private final E myRootValue;
  private final TreeModel<E> myModel;

  public DesktopSwtTreeImpl(E rootValue, TreeModel<E> model) {
    myRootValue = rootValue;
    myModel = model;
  }

  private static final String stubText = "_____STUB_____";

  @Override
  protected org.eclipse.swt.widgets.Tree createSWT(Composite parent) {
    return new org.eclipse.swt.widgets.Tree(parent, packScrollFlags(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.NO_SCROLL));
  }

  @Override
  public void initialize(org.eclipse.swt.widgets.Tree tree) {

    build(tree, myRootValue);

    tree.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        getListenerDispatcher(SelectListener.class).onSelected(getSelectedNode());
      }
    });

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {

        UIAccess.current().give(() -> {
          TreeNode<E> selectedNode = getSelectedNode();
          if (selectedNode != null) {
            myModel.onDoubleClick(DesktopSwtTreeImpl.this, selectedNode);
          }
        });
      }
    });

    tree.addTreeListener(new TreeAdapter() {
      @Override
      public void treeExpanded(TreeEvent e) {
        TreeItem item = (TreeItem)e.item;

        Object loaded = item.getData("loaded");
        if (loaded == Boolean.TRUE) {
          return;
        }

        if (item.getItemCount() == 1 && item.getItems()[0].getText().equals(stubText)) {
          TreeItem stub = item.getItem(0);
          stub.dispose();

          DesktopSwtTreeNode treeNode = (DesktopSwtTreeNode)item.getData("node");

          build(item, (E)treeNode.getValue());

          item.setData("loaded", Boolean.TRUE);
        }
      }
    });

  }

  private void build(Object parent, E value) {
    List<DesktopSwtTreeNode<E>> list = new ArrayList<>();

    myModel.buildChildren(e -> {
      DesktopSwtTreeNode<E> node = new DesktopSwtTreeNode<>(e);
      list.add(node);
      return node;
    }, value);

    Comparator<TreeNode<E>> comparator = myModel.getNodeComparator();
    if (comparator != null) {
      list.sort(comparator);
    }

    for (DesktopSwtTreeNode<E> node : list) {
      final TreeItem item;
      if (parent instanceof org.eclipse.swt.widgets.Tree) {
        item = new TreeItem((org.eclipse.swt.widgets.Tree)parent, SWT.NONE);
      }
      else {
        item = new TreeItem((TreeItem)parent, SWT.NONE);
      }
      if (node.isLeaf()) {
        item.setItemCount(0);
      }
      item.setData("node", node);

      node.setTreeItem(item);
      node.render();

      if (!node.isLeaf()) {
        TreeItem stub = new TreeItem(item, SWT.NONE);
        stub.setText(stubText);
      }
    }

  }

  @Nullable
  @Override
  public TreeNode<E> getSelectedNode() {
    TreeItem[] selection = myComponent.getSelection();
    if (selection.length != 1) {
      return null;
    }
    Object node = selection[0].getData("node");
    if (node instanceof TreeNode) {
      return (TreeNode<E>)node;
    }
    return null;
  }

  @Override
  public void expand(@Nonnull TreeNode<E> node) {

  }
}
