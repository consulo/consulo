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
package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.packageDependencies.ui.TreeExpansionMonitor;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.language.editor.impl.internal.intention.IntentionManagerSettings;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.FilterComponent;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.ref.Ref;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author cdr
 */
public abstract class IntentionSettingsTree {
  private JComponent myComponent;
  private CheckboxTree myTree;
  private FilterComponent myFilter;

  private final Map<IntentionActionMetaData, Boolean> myIntentionToCheckStatus = new HashMap<>();

  protected IntentionSettingsTree() {
    initTree();
  }

  public JTree getTree() {
    return myTree;
  }

  public JComponent getComponent() {
    return myComponent;
  }

  private void initTree() {
    myTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer(true) {
      @Override
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof CheckedTreeNode)) return;
        CheckedTreeNode node = (CheckedTreeNode)value;
        SimpleTextAttributes attributes = node.getUserObject() instanceof IntentionActionMetaData ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        final String text = getNodeText(node);
        final Color background = selected ? UIUtil.getTreeSelectionBackground(selected) : UIUtil.getTreeTextBackground();
        UIUtil.changeBackGround(this, background);
        SearchUtil.appendFragments(myFilter != null ? myFilter.getFilter() : null, text, attributes.getStyle(), attributes.getFgColor(), background, getTextRenderer());
      }
    }, new CheckedTreeNode(null));

    myTree.getSelectionModel().addTreeSelectionListener(e -> {
      TreePath path = e.getPath();
      Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
      selectionChanged(userObject);
    });

    myFilter = new MyFilterComponent();
    myComponent = new JPanel(new BorderLayout());
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree, true);
    myComponent.add(myFilter, BorderLayout.NORTH);
    myComponent.add(scrollPane, BorderLayout.CENTER);

    myFilter.reset();
  }

  protected abstract void selectionChanged(Object selected);

  protected abstract List<IntentionActionMetaData> filterModel(String filter, final boolean force);

  public void filter(List<IntentionActionMetaData> intentionsToShow) {
    refreshCheckStatus((CheckedTreeNode)myTree.getModel().getRoot());
    reset(intentionsToShow);
  }

  public void reset() {
    resetCheckStatus();
    reset(IntentionManagerSettings.getInstance().getMetaData());
  }

  private void resetCheckStatus() {
    myIntentionToCheckStatus.clear();
    IntentionManagerSettings manager = IntentionManagerSettings.getInstance();
    for (IntentionActionMetaData metaData : manager.getMetaData()) {
      myIntentionToCheckStatus.put(metaData, manager.isEnabled(metaData));
    }
  }

  private void reset(List<IntentionActionMetaData> intentionsToShow) {
    CheckedTreeNode root = new CheckedTreeNode(null);
    final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
    intentionsToShow = sort(intentionsToShow);

    for (final IntentionActionMetaData metaData : intentionsToShow) {
      String[] category = metaData.myCategory;
      CheckedTreeNode node = root;
      for (final String name : category) {
        CheckedTreeNode child = findChild(node, name);
        if (child == null) {
          CheckedTreeNode newChild = new CheckedTreeNode(name);
          treeModel.insertNodeInto(newChild, node, node.getChildCount());
          child = newChild;
        }
        node = child;
      }
      CheckedTreeNode newChild = new CheckedTreeNode(metaData);
      treeModel.insertNodeInto(newChild, node, node.getChildCount());
    }
    resetCheckMark(root);
    treeModel.setRoot(root);
    treeModel.nodeChanged(root);
    TreeUtil.expandAll(myTree);
    myTree.setSelectionRow(0);
  }

  public void selectIntention(String familyName) {
    final CheckedTreeNode child = findChildRecursively(getRoot(), familyName);
    if (child != null) {
      final TreePath path = new TreePath(child.getPath());
      // focus after select, due some components can grab focus
      TreeUtil.selectPath(myTree, path).doWhenDone(() -> myTree.requestFocus());
    }
  }

  private static List<IntentionActionMetaData> sort(final List<IntentionActionMetaData> intentionsToShow) {
    List<IntentionActionMetaData> copy = new ArrayList<>(intentionsToShow);
    Collections.sort(copy, (data1, data2) -> {
      String[] category1 = data1.myCategory;
      String[] category2 = data2.myCategory;
      int result = ArrayUtil.lexicographicCompare(category1, category2);
      if (result != 0) {
        return result;
      }
      return data1.getActionText().compareTo(data2.getActionText());
    });
    return copy;
  }

  private CheckedTreeNode getRoot() {
    return (CheckedTreeNode)myTree.getModel().getRoot();
  }

  private boolean resetCheckMark(final CheckedTreeNode root) {
    Object userObject = root.getUserObject();
    if (userObject instanceof IntentionActionMetaData) {
      IntentionActionMetaData metaData = (IntentionActionMetaData)userObject;
      Boolean b = myIntentionToCheckStatus.get(metaData);
      boolean enabled = b != null && b.booleanValue();
      root.setChecked(enabled);
      return enabled;
    }
    else {
      root.setChecked(false);
      visitChildren(root, node -> {
        if (resetCheckMark(node)) {
          root.setChecked(true);
        }
      });
      return root.isChecked();
    }
  }

  private static CheckedTreeNode findChild(CheckedTreeNode node, final String name) {
    final Ref<CheckedTreeNode> found = new Ref<>();
    visitChildren(node, node1 -> {
      String text = getNodeText(node1);
      if (name.equals(text)) {
        found.set(node1);
      }
    });
    return found.get();
  }

  private static CheckedTreeNode findChildRecursively(CheckedTreeNode node, final String name) {
    final Ref<CheckedTreeNode> found = new Ref<>();
    visitChildren(node, node1 -> {
      if (found.get() != null) return;
      final Object userObject = node1.getUserObject();
      if (userObject instanceof IntentionActionMetaData) {
        String text = getNodeText(node1);
        if (name.equals(text)) {
          found.set(node1);
        }
      }
      else {
        final CheckedTreeNode child = findChildRecursively(node1, name);
        if (child != null) {
          found.set(child);
        }
      }
    });
    return found.get();
  }

  private static String getNodeText(CheckedTreeNode node) {
    final Object userObject = node.getUserObject();
    String text;
    if (userObject instanceof String) {
      text = (String)userObject;
    }
    else if (userObject instanceof IntentionActionMetaData) {
      text = ((IntentionActionMetaData)userObject).getActionText();
    }
    else {
      text = "???";
    }
    return text;
  }

  public void apply() {
    CheckedTreeNode root = getRoot();
    apply(root);
  }

  private void refreshCheckStatus(final CheckedTreeNode root) {
    Object userObject = root.getUserObject();
    if (userObject instanceof IntentionActionMetaData) {
      IntentionActionMetaData actionMetaData = (IntentionActionMetaData)userObject;
      myIntentionToCheckStatus.put(actionMetaData, root.isChecked());
    }
    else {
      visitChildren(root, node -> refreshCheckStatus(node));
    }

  }

  private static void apply(CheckedTreeNode root) {
    Object userObject = root.getUserObject();
    if (userObject instanceof IntentionActionMetaData) {
      IntentionActionMetaData actionMetaData = (IntentionActionMetaData)userObject;
      IntentionManagerSettings.getInstance().setEnabled(actionMetaData, root.isChecked());
    }
    else {
      visitChildren(root, node -> apply(node));
    }
  }

  public boolean isModified() {
    return isModified(getRoot());
  }

  private static boolean isModified(CheckedTreeNode root) {
    Object userObject = root.getUserObject();
    if (userObject instanceof IntentionActionMetaData) {
      IntentionActionMetaData actionMetaData = (IntentionActionMetaData)userObject;
      boolean enabled = IntentionManagerSettings.getInstance().isEnabled(actionMetaData);
      return enabled != root.isChecked();
    }
    else {
      final boolean[] modified = new boolean[]{false};
      visitChildren(root, node -> modified[0] |= isModified(node));
      return modified[0];
    }
  }

  public void dispose() {
    myFilter.dispose();
  }

  public void setFilter(String filter) {
    myFilter.setFilter(filter);
  }

  public String getFilter() {
    return myFilter.getFilter();
  }

  interface CheckedNodeVisitor {
    void visit(CheckedTreeNode node);
  }

  private static void visitChildren(CheckedTreeNode node, CheckedNodeVisitor visitor) {
    Enumeration children = node.children();
    while (children.hasMoreElements()) {
      final CheckedTreeNode child = (CheckedTreeNode)children.nextElement();
      visitor.visit(child);
    }
  }

  private class MyFilterComponent extends FilterComponent {
    private final TreeExpansionMonitor<DefaultMutableTreeNode> myExpansionMonitor = TreeExpansionMonitor.install(myTree);

    public MyFilterComponent() {
      super("INTENTION_FILTER_HISTORY", 10);
    }

    @Override
    public void filter() {
      final String filter = getFilter();
      if (filter != null && filter.length() > 0) {
        if (!myExpansionMonitor.isFreeze()) {
          myExpansionMonitor.freeze();
        }
      }
      IntentionSettingsTree.this.filter(filterModel(filter, true));
      if (myTree != null) {
        List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myTree);
        ((DefaultTreeModel)myTree.getModel()).reload();
        TreeUtil.restoreExpandedPaths(myTree, expandedPaths);
      }
      SwingUtilities.invokeLater(() -> {
        myTree.setSelectionRow(0);
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
      });
      TreeUtil.expandAll(myTree);
      if (filter == null || filter.length() == 0) {
        TreeUtil.collapseAll(myTree, 0);
        myExpansionMonitor.restore();
      }
    }

    @Override
    protected void onlineFilter() {
      final String filter = getFilter();
      if (filter != null && filter.length() > 0) {
        if (!myExpansionMonitor.isFreeze()) {
          myExpansionMonitor.freeze();
        }
      }
      IntentionSettingsTree.this.filter(filterModel(filter, true));
      TreeUtil.expandAll(myTree);
      if (filter == null || filter.length() == 0) {
        TreeUtil.collapseAll(myTree, 0);
        myExpansionMonitor.restore();
      }
    }
  }

  public JPanel getToolbarPanel() {
    return myFilter;
  }
}
