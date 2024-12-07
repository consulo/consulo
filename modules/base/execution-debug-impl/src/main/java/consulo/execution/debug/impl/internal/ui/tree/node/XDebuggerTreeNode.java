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

import consulo.application.Application;
import consulo.execution.debug.frame.XDebuggerTreeNodeHyperlink;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author nik
 */
public abstract class XDebuggerTreeNode implements TreeNode {
  protected final XDebuggerTree myTree;
  private final XDebuggerTreeNode myParent;
  private boolean myLeaf;
  protected final SimpleColoredText myText = new SimpleColoredText();
  private Image myIcon;
  private TreePath myPath;

  protected XDebuggerTreeNode(final XDebuggerTree tree, final @Nullable XDebuggerTreeNode parent, final boolean leaf) {
    myParent = parent;
    myLeaf = leaf;
    myTree = tree;
  }

  @Override
  public TreeNode getChildAt(final int childIndex) {
    return isLeaf() ? null : getChildren().get(childIndex);
  }

  @Override
  public int getChildCount() {
    return isLeaf() ? 0 : getChildren().size();
  }

  @Override
  public TreeNode getParent() {
    return myParent;
  }

  @Override
  public int getIndex(@Nonnull TreeNode node) {
    if (isLeaf()) return -1;
    return getChildren().indexOf(node);
  }

  @Override
  public boolean getAllowsChildren() {
    return true;
  }

  @Override
  public boolean isLeaf() {
    return myLeaf;
  }

  @Override
  public Enumeration children() {
    if (isLeaf()) {
      return Collections.emptyEnumeration();
    }
    return Collections.enumeration(getChildren());
  }

  @Nonnull
  public abstract List<? extends TreeNode> getChildren();

  protected void setIcon(Image icon) {
    myIcon = icon;
  }

  public void setLeaf(final boolean leaf) {
    myLeaf = leaf;
  }

  @Nullable
  protected XDebuggerTreeNodeHyperlink getLink() {
    return null;
  }

  @Nonnull
  public SimpleColoredText getText() {
    return myText;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  protected void fireNodeChanged() {
    myTree.getTreeModel().nodeChanged(this);
  }

  protected void fireNodesRemoved(int[] indices, TreeNode[] nodes) {
    if (indices.length > 0) {
      myTree.getTreeModel().nodesWereRemoved(this, indices, nodes);
    }
  }

  protected void fireNodesInserted(Collection<? extends TreeNode> added) {
    if (!added.isEmpty()) {
      myTree.getTreeModel().nodesWereInserted(this, getNodesIndices(added));
    }
  }

  protected TreeNode[] getChildNodes(int[] indices) {
    final TreeNode[] children = new TreeNode[indices.length];
    for (int i = 0; i < indices.length; i++) {
      children[i] = getChildAt(indices[i]);
    }
    return children;
  }

  protected int[] getNodesIndices(@Nullable Collection<? extends TreeNode> children) {
    if (children == null) return ArrayUtil.EMPTY_INT_ARRAY;

    final int[] ints = new int[children.size()];
    int i = 0;
    for (TreeNode node : children) {
      ints[i++] = getIndex(node);
    }
    Arrays.sort(ints);
    return ints;
  }

  protected void fireNodeStructureChanged() {
    fireNodeStructureChanged(this);
  }

  protected void fireNodeStructureChanged(final TreeNode node) {
    myTree.getTreeModel().nodeStructureChanged(node);
  }

  public XDebuggerTree getTree() {
    return myTree;
  }

  public TreePath getPath() {
    if (myPath == null) {
      TreePath path;
      if (myParent == null) {
        path = new TreePath(this);
      }
      else {
        path = myParent.getPath().pathByAddingChild(this);
      }
      myPath = path;
    }
    return myPath;
  }

  @Nonnull
  public abstract List<? extends XDebuggerTreeNode> getLoadedChildren();

  public abstract void clearChildren();

  public void appendToComponent(@Nonnull ColoredTextContainer component) {
    getText().appendToComponent(component);

    XDebuggerTreeNodeHyperlink link = getLink();
    if (link != null) {
      component.append(link.getLinkText(), link.getTextAttributes(), link);
    }
  }

  public void invokeNodeUpdate(Runnable runnable) {
    Application.get().invokeLater(runnable);
  }
}
