/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.impl.internal.frame.WatchInplaceEditor;
import consulo.execution.debug.impl.internal.frame.XWatchesView;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public class WatchesRootNode extends XValueContainerNode<XValueContainer> {
  private final XWatchesView myWatchesView;
  private final List<WatchNodeImpl> myChildren;

  public WatchesRootNode(@Nonnull XDebuggerTree tree,
                         @Nonnull XWatchesView watchesView,
                         @Nonnull XExpression[] expressions) {
    this(tree, watchesView, expressions, null, false);
  }

  public WatchesRootNode(@Nonnull XDebuggerTree tree,
                         @Nonnull XWatchesView watchesView,
                         @Nonnull XExpression[] expressions,
                         @Nullable XStackFrame stackFrame,
                         boolean watchesInVariables) {
    super(tree, null, new XValueContainer() {
      @Override
      public void computeChildren(@Nonnull XCompositeNode node) {
        if (stackFrame != null && watchesInVariables) {
          stackFrame.computeChildren(node);
        }
        else {
          node.addChildren(XValueChildrenList.EMPTY, true);
        }
      }
    });
    setLeaf(false);
    myWatchesView = watchesView;
    myChildren = new ArrayList<>();
    for (XExpression watchExpression : expressions) {
      myChildren.add(new WatchNodeImpl(myTree, this, watchExpression, stackFrame));
    }
  }

  @Nonnull
  @Override
  public List<? extends XValueContainerNode<?>> getLoadedChildren() {
    return ContainerUtil.concat(myChildren, super.getLoadedChildren());
  }

  @Nonnull
  @Override
  public List<? extends TreeNode> getChildren() {
    List<? extends TreeNode> children = super.getChildren();
    return ContainerUtil.concat(myChildren, children);
  }

  /**
   * @deprecated use {@link #getWatchChildren()} instead
   */
  @Nonnull
  public List<? extends WatchNode> getAllChildren() {
    return getWatchChildren();
  }

  @Nonnull
  public List<? extends WatchNode> getWatchChildren() {
    return myChildren;
  }

  @Override
  public void clearChildren() {
    super.clearChildren();
    myChildren.clear();
  }

  public void computeWatches() {
    myChildren.forEach(WatchNodeImpl::computePresentationIfNeeded);
  }

  /**
   * @deprecated Use {@link #addWatchExpression(XStackFrame, XExpression, int, boolean)}
   */
  @Deprecated
  public void addWatchExpression(
    @Nullable XDebuggerEvaluator evaluator,
    @Nonnull XExpression expression,
    int index,
    boolean navigateToWatchNode
  ) {
    addWatchExpression((XStackFrame)null, expression, index, navigateToWatchNode);
  }

  public void addWatchExpression(
    @Nullable XStackFrame stackFrame,
    @Nonnull XExpression expression,
    int index,
    boolean navigateToWatchNode
  ) {
    WatchNodeImpl message = new WatchNodeImpl(myTree, this, expression, stackFrame);
    if (index == -1) {
      myChildren.add(message);
      index = myChildren.size() - 1;
    }
    else {
      myChildren.add(index, message);
    }
    fireNodeInserted(index);
    TreeUtil.selectNode(myTree, message);
    if (navigateToWatchNode) {
      myTree.scrollPathToVisible(message.getPath());
    }
  }

  private void fireNodeInserted(int index) {
    myTree.getTreeModel().nodesWereInserted(this, new int[]{index});
  }

  public int removeChildNode(XDebuggerTreeNode node) {
    return removeChildNode(myChildren, node);
  }

  public void removeChildren(Collection<? extends XDebuggerTreeNode> nodes) {
    int[] indices = getNodesIndices(nodes);
    TreeNode[] removed = getChildNodes(indices);
    myChildren.removeAll(nodes);
    fireNodesRemoved(indices, removed);
  }

  public void removeAllChildren() {
    myChildren.clear();
    fireNodeStructureChanged();
  }

  public void moveUp(WatchNode node) {
    int index = getIndex(node);
    if (index > 0) {
      ContainerUtil.swapElements(myChildren, index, index - 1);
    }
    fireNodeStructureChanged();
    getTree().setSelectionRow(index - 1);
  }

  public void moveDown(WatchNode node) {
    int index = getIndex(node);
    if (index < myChildren.size() - 1) {
      ContainerUtil.swapElements(myChildren, index, index + 1);
    }
    fireNodeStructureChanged();
    getTree().setSelectionRow(index + 1);
  }

  public void addNewWatch() {
    editWatch(null);
  }

  public void editWatch(@Nullable WatchNodeImpl node) {
    WatchNodeImpl messageNode;
    int index = node != null ? myChildren.indexOf(node) : -1;
    if (index == -1) {
      int selectedIndex = myChildren.indexOf(ArrayUtil.getFirstElement(myTree.getSelectedNodes(WatchNodeImpl.class, null)));
      int targetIndex = selectedIndex == - 1 ? myChildren.size() : selectedIndex + 1;
      messageNode = new WatchNodeImpl(myTree, this, XExpression.EMPTY_EXPRESSION, null);
      myChildren.add(targetIndex, messageNode);
      fireNodeInserted(targetIndex);
      getTree().setSelectionRows(ArrayUtil.EMPTY_INT_ARRAY);
    }
    else {
      messageNode = node;
    }
    new WatchInplaceEditor(this, myWatchesView, messageNode, node).show();
  }
}
