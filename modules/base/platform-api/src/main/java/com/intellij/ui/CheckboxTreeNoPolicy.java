/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.ThreeStateCheckBox;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * This is parent of {@link com.intellij.ui.CheckboxTreeBase} but did not support policy, for support it override {@link #adjustParentsAndChildren}
 */
public class CheckboxTreeNoPolicy extends Tree {

  public CheckboxTreeNoPolicy() {
    this(new CheckboxTreeCellRendererBase(), null);
  }

  public CheckboxTreeNoPolicy(CheckboxTreeCellRendererBase cellRenderer, @Nullable CheckedTreeNode root) {
    setRootVisible(false);
    setShowsRootHandles(true);
    setLineStyleAngled();
    TreeUtil.installActions(this);

    installRenderer(cellRenderer);

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (isToggleEvent(e)) {
          TreePath treePath = getLeadSelectionPath();
          if (treePath == null) return;
          final Object o = treePath.getLastPathComponent();
          if (!(o instanceof CheckedTreeNode)) return;
          CheckedTreeNode firstNode = (CheckedTreeNode)o;
          boolean checked = toggleNode(firstNode);

          TreePath[] selectionPaths = getSelectionPaths();
          for (int i = 0; selectionPaths != null && i < selectionPaths.length; i++) {
            final TreePath selectionPath = selectionPaths[i];
            final Object o1 = selectionPath.getLastPathComponent();
            if (!(o1 instanceof CheckedTreeNode)) continue;
            CheckedTreeNode node = (CheckedTreeNode)o1;
            checkNode(node, checked);
            ((DefaultTreeModel)getModel()).nodeChanged(node);
          }

          e.consume();
        }
      }
    });

    setSelectionRow(0);
    if (root != null) {
      setModel(new DefaultTreeModel(root));
    }
  }

  public void installRenderer(final CheckboxTreeCellRendererBase cellRenderer) {
    setCellRenderer(cellRenderer);
    new ClickListener() {
      @Override
      public boolean onClick(MouseEvent e, int clickCount) {
        int row = getRowForLocation(e.getX(), e.getY());
        if (row < 0) return false;
        final Object o = getPathForRow(row).getLastPathComponent();
        if (!(o instanceof CheckedTreeNode)) return false;
        Rectangle rowBounds = getRowBounds(row);
        cellRenderer.setBounds(rowBounds);
        Rectangle checkBounds = cellRenderer.myCheckbox.getBounds();
        checkBounds.setLocation(rowBounds.getLocation());

        if (checkBounds.height == 0) checkBounds.height = checkBounds.width = rowBounds.height;

        final CheckedTreeNode node = (CheckedTreeNode)o;
        if (checkBounds.contains(e.getPoint())) {
          if (node.isEnabled()) {
            toggleNode(node);
            setSelectionRow(row);
            return true;
          }
        }
        else if (clickCount > 1) {
          onDoubleClick(node);
          return true;
        }
        return false;
      }
    }.installOn(this);
  }

  protected void onDoubleClick(final CheckedTreeNode node) {
  }

  protected boolean isToggleEvent(KeyEvent e) {
    return e.getKeyCode() == KeyEvent.VK_SPACE;
  }

  protected boolean toggleNode(CheckedTreeNode node) {
    boolean checked = !node.isChecked();
    checkNode(node, checked);

    // notify model listeners about model change
    final TreeModel model = getModel();
    model.valueForPathChanged(new TreePath(node.getPath()), node.getUserObject());

    return checked;
  }

  /**
   * Collect checked leaf nodes of the type {@code nodeType} and that are accepted by
   * {@code filter}
   *
   * @param nodeType the type of userobject to consider
   * @param filter   the filter (if null all nodes are accepted)
   * @param <T>      the type of the node
   * @return an array of collected nodes
   */
  @SuppressWarnings("unchecked")
  public <T> T[] getCheckedNodes(final Class<T> nodeType, @Nullable final NodeFilter<T> filter) {
    final ArrayList<T> nodes = new ArrayList<T>();
    final Object root = getModel().getRoot();
    if (!(root instanceof CheckedTreeNode)) {
      throw new IllegalStateException(
              "The root must be instance of the " + CheckedTreeNode.class.getName() + ": " + root.getClass().getName());
    }
    new Object() {
      @SuppressWarnings("unchecked")
      public void collect(CheckedTreeNode node) {
        if (node.isLeaf()) {
          Object userObject = node.getUserObject();
          if (node.isChecked() && userObject != null && nodeType.isAssignableFrom(userObject.getClass())) {
            final T value = (T)userObject;
            if (filter != null && !filter.accept(value)) return;
            nodes.add(value);
          }
        }
        else {
          for (int i = 0; i < node.getChildCount(); i++) {
            final TreeNode child = node.getChildAt(i);
            if (child instanceof CheckedTreeNode) {
              collect((CheckedTreeNode)child);
            }
          }
        }
      }
    }.collect((CheckedTreeNode)root);
    T[] result = (T[])Array.newInstance(nodeType, nodes.size());
    nodes.toArray(result);
    return result;
  }


  @Override
  public int getToggleClickCount() {
    // to prevent node expanding/collapsing on checkbox toggling
    return -1;
  }

  protected void checkNode(CheckedTreeNode node, boolean checked) {
    adjustParentsAndChildren(node, checked);
    repaint();
  }

  protected void onNodeStateChanged(CheckedTreeNode node) {

  }

  protected void nodeStateWillChange(CheckedTreeNode node) {

  }

  protected void adjustParentsAndChildren(final CheckedTreeNode node, final boolean checked) {
    changeNodeState(node, checked);

    repaint();
  }

  protected void changeNodeState(final CheckedTreeNode node, final boolean checked) {
    if (node.isChecked() != checked) {
      nodeStateWillChange(node);
      node.setChecked(checked);
      onNodeStateChanged(node);
    }
  }

  protected void checkOrUncheckChildren(final CheckedTreeNode node, boolean value) {
    final Enumeration children = node.children();
    while (children.hasMoreElements()) {
      final Object o = children.nextElement();
      if (!(o instanceof CheckedTreeNode)) continue;
      CheckedTreeNode child = (CheckedTreeNode)o;
      changeNodeState(child, value);
      checkOrUncheckChildren(child, value);
    }
  }

  protected void adjustParents(final CheckedTreeNode node, final boolean checked) {
    TreeNode parentNode = node.getParent();
    if (!(parentNode instanceof CheckedTreeNode)) return;
    CheckedTreeNode parent = (CheckedTreeNode)parentNode;

    if (!checked && isAllChildrenUnchecked(parent)) {
      changeNodeState(parent, false);
      adjustParents(parent, false);
    }
    else if (checked && isAllChildrenChecked(parent)) {
      changeNodeState(parent, true);
      adjustParents(parent, true);
    }
  }

  private static boolean isAllChildrenUnchecked(final CheckedTreeNode node) {
    for (int i = 0; i < node.getChildCount(); i++) {
      final TreeNode o = node.getChildAt(i);
      if ((o instanceof CheckedTreeNode) && ((CheckedTreeNode)o).isChecked()) {
        return false;
      }
    }
    return true;
  }

  private static boolean isAllChildrenChecked(final CheckedTreeNode node) {
    for (int i = 0; i < node.getChildCount(); i++) {
      final TreeNode o = node.getChildAt(i);
      if ((o instanceof CheckedTreeNode) && !((CheckedTreeNode)o).isChecked()) {
        return false;
      }
    }
    return true;
  }

  public static class CheckboxTreeCellRendererBase extends JPanel implements TreeCellRenderer {
    private final ColoredTreeCellRenderer myTextRenderer;
    public final ThreeStateCheckBox myCheckbox;
    private final boolean myUsePartialStatusForParentNodes;

    public CheckboxTreeCellRendererBase(boolean opaque) {
      this(opaque, true);
    }

    public CheckboxTreeCellRendererBase(boolean opaque, final boolean usePartialStatusForParentNodes) {
      super(new BorderLayout());
      myUsePartialStatusForParentNodes = usePartialStatusForParentNodes;
      myCheckbox = new ThreeStateCheckBox();
      myCheckbox.setSelected(false);
      myCheckbox.setThirdStateEnabled(false);
      myTextRenderer = new ColoredTreeCellRenderer() {
        @RequiredUIAccess
        @Override
        public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) { }
      };
      myTextRenderer.setOpaque(opaque);
      add(myCheckbox, BorderLayout.WEST);
      add(myTextRenderer, BorderLayout.CENTER);
    }

    public CheckboxTreeCellRendererBase() {
      this(true);
    }

    @Override
    @RequiredUIAccess
    public final Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      invalidate();
      if (value instanceof CheckedTreeNode) {
        CheckedTreeNode node = (CheckedTreeNode)value;

        NodeState state = getNodeStatus(node);
        myCheckbox.setVisible(true);
        myCheckbox.setSelected(state != NodeState.CLEAR);
        myCheckbox.setEnabled(node.isEnabled() && state != NodeState.PARTIAL);
        myCheckbox.setOpaque(false);
        myCheckbox.setBackground(null);
        setBackground(null);
      }
      else {
        myCheckbox.setVisible(false);
      }
      myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

      if (UIUtil.isUnderGTKLookAndFeel()) {
        final Color background = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();
        UIUtil.changeBackGround(this, background);
      }
      else if (UIUtil.isUnderNimbusLookAndFeel()) {
        UIUtil.changeBackGround(this, UIUtil.TRANSPARENT_COLOR);
      }
      customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      revalidate();

      return this;
    }

    private NodeState getNodeStatus(final CheckedTreeNode node) {
      final boolean checked = node.isChecked();
      if (node.getChildCount() == 0 || !myUsePartialStatusForParentNodes) return checked ? NodeState.FULL : NodeState.CLEAR;

      NodeState result = null;

      for (int i = 0; i < node.getChildCount(); i++) {
        TreeNode child = node.getChildAt(i);
        NodeState childStatus = child instanceof CheckedTreeNode? getNodeStatus((CheckedTreeNode)child) :
                                checked? NodeState.FULL : NodeState.CLEAR;
        if (childStatus == NodeState.PARTIAL) return NodeState.PARTIAL;
        if (result == null) {
          result = childStatus;
        }
        else if (result != childStatus) {
          return NodeState.PARTIAL;
        }
      }

      return result == null ? NodeState.CLEAR : result;
    }

    /**
     * Should be implemented by concrete implementations.
     * This method is invoked only for customization of component.
     * All component attributes are cleared when this method is being invoked.
     * Note that in general case <code>value</code> is not an instance of CheckedTreeNode.
     */
    public void customizeRenderer(JTree tree,
                                  Object value,
                                  boolean selected,
                                  boolean expanded,
                                  boolean leaf,
                                  int row,
                                  boolean hasFocus) {
      if (value instanceof CheckedTreeNode) {
        customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      }
    }

    /**
     * @see CheckboxTreeCellRendererBase#customizeRenderer(javax.swing.JTree, Object, boolean, boolean, boolean, int, boolean)
     * @deprecated
     */
    @Deprecated
    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
    }

    public ColoredTreeCellRenderer getTextRenderer() {
      return myTextRenderer;
    }

    public JCheckBox getCheckbox() {
      return myCheckbox;
    }
  }


  public enum NodeState {
    FULL, CLEAR, PARTIAL
  }
}
