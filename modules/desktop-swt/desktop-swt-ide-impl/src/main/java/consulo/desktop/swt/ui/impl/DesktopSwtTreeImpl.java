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

import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtTreeImpl<E> extends SWTComponentDelegate<org.eclipse.swt.widgets.Tree> implements Tree<E> {
  private @Nullable TransferHandler myTransferHandler;
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
        getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent(DesktopSwtTreeImpl.this, getSelectedNode()));
      }
    });

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {

        UIAccess.current().give(() -> {
          TreeNode<E> selectedNode = getSelectedNode();
          if (selectedNode != null) {
            getListenerDispatcher(TreeDoubleClickEvent.class).onEvent(new TreeDoubleClickEvent<>(DesktopSwtTreeImpl.this, selectedNode));

            myModel.onDoubleClick(DesktopSwtTreeImpl.this, selectedNode);
          }
        });
      }
    });

    tree.addTreeListener(new TreeAdapter() {
      @Override
      public void treeExpanded(TreeEvent e) {
        TreeItem item = (TreeItem)e.item;

        loadChildren(item);

        fireExpand(item);
      }

      @Override
      public void treeCollapsed(TreeEvent e) {
        fireCollapse((TreeItem)e.item);
      }
    });

  }

  /**
   * Builds the level below the item when it holds nothing but the stub. The children of a node are built when
   * it is first opened, and {@link TreeItem#setExpanded(boolean)} runs neither this nor the tree listener, so
   * an expand driven from the api has to come through here as well.
   */
  private void loadChildren(TreeItem item) {
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

  /**
   * Opens the item and the levels below it, building each as it goes. The widget sends no event of its own for
   * this, so the listeners are told here.
   */
  private void expandItem(TreeItem item, int depth) {
    if (depth <= 0 || item.isDisposed()) {
      return;
    }

    loadChildren(item);

    // a leaf has nothing to open, and the widget would take the call all the same
    if (item.getItemCount() == 0) {
      return;
    }

    if (!item.getExpanded()) {
      item.setExpanded(true);

      fireExpand(item);
    }

    for (TreeItem child : item.getItems()) {
      expandItem(child, depth - 1);
    }
  }

  private void collapseItem(TreeItem item) {
    if (item.isDisposed()) {
      return;
    }

    for (TreeItem child : item.getItems()) {
      collapseItem(child);
    }

    if (item.getExpanded()) {
      item.setExpanded(false);

      fireCollapse(item);
    }
  }

  private void fireExpand(TreeItem item) {
    if (item.getData("node") instanceof TreeNode node) {
      getListenerDispatcher(TreeExpandEvent.class).onEvent(new TreeExpandEvent(this, node));
    }
  }

  private void fireCollapse(TreeItem item) {
    if (item.getData("node") instanceof TreeNode node) {
      getListenerDispatcher(TreeCollapseEvent.class).onEvent(new TreeCollapseEvent(this, node));
    }
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
      TreeItem item;
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

  @Override
  public @Nullable TreeNode<E> getSelectedNode() {
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
  public CompletableFuture<?> expand(TreeNode<E> node, int depth) {
    if (!(node instanceof DesktopSwtTreeNode<E> swtNode)) {
      return CompletableFuture.completedFuture(null);
    }

    TreeItem item = swtNode.getTreeItem();
    if (item == null || item.isDisposed()) {
      return CompletableFuture.completedFuture(null);
    }

    // the item is only shown when the ones above it are open
    for (TreeItem parent = item.getParentItem(); parent != null; parent = parent.getParentItem()) {
      if (!parent.getExpanded()) {
        parent.setExpanded(true);

        fireExpand(parent);
      }
    }

    expandItem(item, depth);

    // every level is built and opened right here, on the ui thread
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean isExpandCollapseAllSupported() {
    return true;
  }

  @Override
  public CompletableFuture<?> expandAll(int depth) {
    for (TreeItem item : myComponent.getItems()) {
      expandItem(item, depth);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<?> collapseAll() {
    for (TreeItem item : myComponent.getItems()) {
      collapseItem(item);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
    if (!(node instanceof DesktopSwtTreeNode<E> swtNode)) {
      return;
    }

    TreeItem item = swtNode.getTreeItem();
    if (item == null || item.isDisposed()) {
      return;
    }

    swtNode.render();

    if (refreshChildren) {
      for (TreeItem child : item.getItems()) {
        child.dispose();
      }

      item.setData("loaded", Boolean.TRUE);
      build(item, swtNode.getValue());
    }
  }

  @Override
  public CompletableFuture<?> refreshAll() {
    for (TreeItem item : myComponent.getItems()) {
      item.dispose();
    }

    build(myComponent, myRootValue);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void setTransferHandler(@Nullable TransferHandler handler) {
      myTransferHandler = handler;
  }

  @Override
  public @Nullable TransferHandler getTransferHandler() {
      return myTransferHandler;
  }
}
