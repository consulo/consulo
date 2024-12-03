/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.AllIcons;
import consulo.dataContext.DataManager;
import consulo.application.ui.UISettings;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ui.ex.keymap.Keymap;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapImpl;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.application.util.registry.Registry;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;

public class ActionsTree {
  private static final Image CLOSE_ICON = AllIcons.Nodes.Folder;

  private final JTree myTree;
  private DefaultMutableTreeNode myRoot;
  private final JScrollPane myComponent;
  private Keymap myKeymap;
  private KeymapGroupImpl myMainGroup = new KeymapGroupImpl("", null, null);
  private boolean myShowBoundActions = Registry.is("keymap.show.alias.actions");

  private static final String ROOT = "ROOT";

  private String myFilter = null;
  private final DefaultTreeModel myModel;

  public ActionsTree(@Nonnull Disposable disposable) {
    myRoot = new DefaultMutableTreeNode(ROOT);

    myModel = new DefaultTreeModel(myRoot);
    myTree = new Tree(new AsyncTreeModel(myModel, disposable)) {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        Rectangle visibleRect = getVisibleRect();
        Rectangle clip = g.getClipBounds();
        for (int row = 0; row < getRowCount(); row++) {
          Rectangle rowBounds = getRowBounds(row);
          rowBounds.x = 0;
          rowBounds.width = Integer.MAX_VALUE;

          if (rowBounds.intersects(clip)) {
            Object node = getPathForRow(row).getLastPathComponent();

            if (node instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
              Object data = defaultMutableTreeNode.getUserObject();
              Rectangle fullRowRect = new Rectangle(visibleRect.x, rowBounds.y, visibleRect.width, rowBounds.height);
              paintRowData(this, data, fullRowRect, (Graphics2D)g);
            }
          }
        }
        
      }
    };
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);

    myTree.setCellRenderer(new KeymapsRenderer());

    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    myComponent = ScrollPaneFactory.createScrollPane(myTree,
                                                     ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public void addTreeSelectionListener(TreeSelectionListener l) {
    myTree.getSelectionModel().addTreeSelectionListener(l);
  }

  @Nullable
  private Object getSelectedObject() {
    TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath == null) return null;
    return ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
  }

  @Nullable
  public String getSelectedActionId() {
    Object userObject = getSelectedObject();
    if (userObject instanceof String actionId) return actionId;
    if (userObject instanceof QuickList quickList) return quickList.getActionId();
    return null;
  }

  @Nullable
  public QuickList getSelectedQuickList() {
    Object userObject = getSelectedObject();
    if (!(userObject instanceof QuickList)) return null;
    return (QuickList)userObject;
  }

  public void reset(Keymap keymap, final QuickList[] allQuickLists) {
    reset(keymap, allQuickLists, myFilter, null);
  }

  public KeymapGroupImpl getMainGroup() {
    return myMainGroup;
  }

  public JTree getTree(){
    return myTree;
  }

  public void filter(final String filter, final QuickList[] currentQuickListIds) {
    myFilter = filter;
    reset(myKeymap, currentQuickListIds, filter, null);
  }

  private void reset(final Keymap keymap, final QuickList[] allQuickLists, String filter, @Nullable KeyboardShortcut shortcut) {
    myKeymap = keymap;

    final PathsKeeper pathsKeeper = new PathsKeeper();
    pathsKeeper.storePaths();

    myRoot.removeAllChildren();

    ActionManager actionManager = ActionManager.getInstance();
    Project project = DataManager.getInstance().getDataContext(myComponent).getData(Project.KEY);
    KeymapGroupImpl mainGroup = ActionsTreeUtil.createMainGroup(project, myKeymap, allQuickLists, filter, true, filter != null && filter.length() > 0 ?
                                                                                                                ActionsTreeUtil.isActionFiltered(filter, true) :
                                                                                                                (shortcut != null ? ActionsTreeUtil.isActionFiltered(actionManager, myKeymap, shortcut) : null));
    if ((filter != null && filter.length() > 0 || shortcut != null) && mainGroup.initIds().isEmpty()){
      mainGroup = ActionsTreeUtil.createMainGroup(project, myKeymap, allQuickLists, filter, false, filter != null && filter.length() > 0 ?
                                                                                                   ActionsTreeUtil.isActionFiltered(filter, false) :
                                                                                                   ActionsTreeUtil.isActionFiltered(actionManager, myKeymap, shortcut));
    }
    myRoot = ActionsTreeUtil.createNode(mainGroup);
    myMainGroup = mainGroup;
    myModel.setRoot(myRoot);
    myModel.nodeStructureChanged(myRoot);

    pathsKeeper.restorePaths();
  }

  public void filterTree(final KeyboardShortcut keyboardShortcut, final QuickList [] currentQuickListIds) {
    reset(myKeymap, currentQuickListIds, myFilter, keyboardShortcut);
  }

  private static boolean isActionChanged(String actionId, Keymap oldKeymap, Keymap newKeymap) {
    if (!newKeymap.canModify()) return false;

    Shortcut[] oldShortcuts = oldKeymap.getShortcuts(actionId);
    Shortcut[] newShortcuts = newKeymap.getShortcuts(actionId);
    return !Comparing.equal(oldShortcuts, newShortcuts);
  }

  private static boolean isGroupChanged(KeymapGroupImpl group, Keymap oldKeymap, Keymap newKeymap) {
    if (!newKeymap.canModify()) return false;

    ArrayList children = group.getChildren();
    for (Object child : children) {
      if (child instanceof KeymapGroupImpl keymapGroup) {
        if (isGroupChanged(keymapGroup, oldKeymap, newKeymap)) {
          return true;
        }
      }
      else if (child instanceof String actionId) {
        if (isActionChanged(actionId, oldKeymap, newKeymap)) {
          return true;
        }
      }
      else if (child instanceof QuickList quickList) {
        String actionId = quickList.getActionId();
        if (isActionChanged(actionId, oldKeymap, newKeymap)) {
          return true;
        }
      }
    }
    return false;
  }

  public void selectAction(String actionId) {
    final JTree tree = myTree;

    String path = myMainGroup.getActionQualifiedPath(actionId);
    if (path == null) {
      return;
    }
    final DefaultMutableTreeNode node = getNodeForPath(path);
    if (node == null) {
      return;
    }

    TreeUtil.selectInTree(node, true, tree);
  }

  @Nullable
  private DefaultMutableTreeNode getNodeForPath(String path) {
    Enumeration enumeration = ((DefaultMutableTreeNode)myTree.getModel().getRoot()).preorderEnumeration();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
      if (Comparing.equal(getPath(node), path)) {
        return node;
      }
    }
    return null;
  }

  private ArrayList<DefaultMutableTreeNode> getNodesByPaths(ArrayList<String> paths){
    final ArrayList<DefaultMutableTreeNode> result = new ArrayList<>();
    Enumeration enumeration = ((DefaultMutableTreeNode)myTree.getModel().getRoot()).preorderEnumeration();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumeration.nextElement();
      final String path = getPath(node);
      if (paths.contains(path)) {
        result.add(node);
      }
    }
    return result;
  }

  @Nullable
  private String getPath(DefaultMutableTreeNode node) {
    final Object userObject = node.getUserObject();
    if (userObject instanceof String actionId) {
      final TreeNode parent = node.getParent();
      if (parent instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
        final Object object = defaultMutableTreeNode.getUserObject();
        if (object instanceof KeymapGroupImpl keymapGroup) {
          return keymapGroup.getActionQualifiedPath(actionId);
        }
      }

      return myMainGroup.getActionQualifiedPath(actionId);
    }
    if (userObject instanceof KeymapGroupImpl keymapGroup) {
      return keymapGroup.getQualifiedPath();
    }
    if (userObject instanceof QuickList quickList) {
      return quickList.getDisplayName();
    }
    return null;
  }

  public static Image getEvenIcon(@Nullable Image icon) {
    if(icon == null) {
      return Image.empty(Image.DEFAULT_ICON_SIZE);
    }
    return icon;
  }

  private class PathsKeeper {
    private ArrayList<String> myPathsToExpand;
    private ArrayList<String> mySelectionPaths;

    public void storePaths() {
      myPathsToExpand = new ArrayList<>();
      mySelectionPaths = new ArrayList<>();

      DefaultMutableTreeNode root = (DefaultMutableTreeNode)myTree.getModel().getRoot();

      TreePath path = new TreePath(root.getPath());
      if (myTree.isPathSelected(path)){
        addPathToList(root, mySelectionPaths);
      }
      if (myTree.isExpanded(path) || root.getChildCount() == 0){
        addPathToList(root, myPathsToExpand);
        _storePaths(root);
      }
    }

    private void addPathToList(DefaultMutableTreeNode root, ArrayList<String> list) {
      String path = getPath(root);
      if (!StringUtil.isEmpty(path)) {
        list.add(path);
      }
    }

    private void _storePaths(DefaultMutableTreeNode root) {
      ArrayList<TreeNode> childNodes = childrenToArray(root);
      for (final Object childNode1 : childNodes) {
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)childNode1;
        TreePath path = new TreePath(childNode.getPath());
        if (myTree.isPathSelected(path)) {
          addPathToList(childNode, mySelectionPaths);
        }
        if ((myTree.isExpanded(path) || childNode.getChildCount() == 0) && !childNode.isLeaf()) {
          addPathToList(childNode, myPathsToExpand);
          _storePaths(childNode);
        }
      }
    }

    public void restorePaths() {
      final ArrayList<DefaultMutableTreeNode> nodesToExpand = getNodesByPaths(myPathsToExpand);
      for (DefaultMutableTreeNode node : nodesToExpand) {
        myTree.expandPath(new TreePath(node.getPath()));
      }

      if (myTree.getSelectionModel().getSelectionCount() == 0) {
        final ArrayList<DefaultMutableTreeNode> nodesToSelect = getNodesByPaths(mySelectionPaths);
        if (!nodesToSelect.isEmpty()) {
          for (DefaultMutableTreeNode node : nodesToSelect) {
            TreeUtil.selectInTree(node, false, myTree);
          }
        }
        else {
          myTree.setSelectionRow(0);
        }
      }
    }


    private ArrayList<TreeNode> childrenToArray(DefaultMutableTreeNode node) {
      ArrayList<TreeNode> arrayList = new ArrayList<>();
      for(int i = 0; i < node.getChildCount(); i++){
        arrayList.add(node.getChildAt(i));
      }
      return arrayList;
    }
  }

  private class KeymapsRenderer extends ColoredTreeCellRenderer {
    public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      final boolean showIcons = UISettings.getInstance().SHOW_ICONS_IN_MENUS;
      Keymap originalKeymap = myKeymap != null ? myKeymap.getParent() : null;
      Image icon = null;
      String text;
      boolean bound = false;

      if (value instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
        Object userObject = defaultMutableTreeNode.getUserObject();
        boolean changed;
        if (userObject instanceof KeymapGroupImpl group) {
          text = group.getName();

          changed = originalKeymap != null && isGroupChanged(group, originalKeymap, myKeymap);
          icon = group.getIcon();
          if (icon == null){
            icon = CLOSE_ICON;
          }
        }
        else if (userObject instanceof String actionId) {
          bound = myShowBoundActions && ((KeymapImpl)myKeymap).isActionBound(actionId);
          AnAction action = ActionManager.getInstance().getActionOrStub(actionId);
          if (action != null) {
            text = action.getTemplatePresentation().getText();
            if (text == null || text.length() == 0) { //fill dynamic presentation gaps
              text = actionId;
            }
            Image actionIcon = action.getTemplatePresentation().getIcon();
            if (actionIcon != null) {
              icon = actionIcon;
            }
          }
          else {
            text = actionId;
          }
          changed = originalKeymap != null && isActionChanged(actionId, originalKeymap, myKeymap);
        }
        else if (userObject instanceof QuickList list) {
          icon = AllIcons.Actions.QuickList;
          text = list.getDisplayName();

          changed = originalKeymap != null && isActionChanged(list.getActionId(), originalKeymap, myKeymap);
        }
        else if (userObject instanceof AnSeparator) {
          // TODO[vova,anton]: beautify
          changed = false;
          text = "-------------";
        }
        else {
          throw new IllegalArgumentException("unknown userObject: " + userObject);
        }

        if (showIcons) {
          setIcon(ActionsTree.getEvenIcon(icon));
        }

        Color foreground;
        if (selected) {
          foreground = UIUtil.getTreeSelectionForeground();
        }
        else {
          if (changed) {
            foreground = JBColor.BLUE;
          }
          else {
            foreground = UIUtil.getTreeForeground();
          }

          if (bound) {
            foreground = JBColor.MAGENTA;
          }
        }
        SearchUtil.appendFragments(myFilter, text, Font.PLAIN, foreground,
                                   selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground(), this);
      }
    }
  }
  
  private void paintRowData(Tree tree, Object data, Rectangle bounds, Graphics2D g) {
    Shortcut[] shortcuts;
    if (data instanceof String actionId) {
      shortcuts = myKeymap.getShortcuts(actionId);
    }
    else if (data instanceof QuickList quickList) {
      shortcuts = myKeymap.getShortcuts(quickList.getActionId());
    }
    else {
      shortcuts = null;
    }

    if (shortcuts != null && shortcuts.length > 0) {
      int totalWidth = 0;
      final FontMetrics metrics = tree.getFontMetrics(tree.getFont());
      for (Shortcut shortcut : shortcuts) {
        totalWidth += metrics.stringWidth(KeymapUtil.getShortcutText(shortcut));
        totalWidth += 10;
      }
      totalWidth -= 5;

      int x = bounds.x + bounds.width - totalWidth;
      int fontHeight = (int)metrics.getMaxCharBounds(g).getHeight();

      Color c1 = new Color(234, 200, 162);
      Color c2 = new Color(208, 200, 66);

      g.translate(0, bounds.y - 1);
      
      for (Shortcut shortcut : shortcuts) {
        int width = metrics.stringWidth(KeymapUtil.getShortcutText(shortcut));
        UIUtil.drawSearchMatch(g, x, x + width, bounds.height, c1, c2);
        g.setColor(Gray._50);
        g.drawString(KeymapUtil.getShortcutText(shortcut), x, fontHeight);

        x += width;
        x += 10;
      }
      g.translate(0, -bounds.y + 1);
    }
  }
}
