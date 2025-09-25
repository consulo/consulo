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

import consulo.application.ui.UISettings;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ActionsTree {
    private class MyRenderer extends CellRendererPanel implements TreeCellRenderer {
        final KeymapsRenderer myNodeRender = new KeymapsRenderer();
        final JPanel myShortcutPanel = new NonOpaquePanel(new HorizontalLayout(6));
        private final BooleanSupplier myUseUnicodeCharactersForShortcutsGetter;

        MyRenderer(BooleanSupplier useUnicodeCharactersForShortcutsGetter) {
            myUseUnicodeCharactersForShortcutsGetter = useUnicodeCharactersForShortcutsGetter;
            setLayout(new BorderLayout());
            add(BorderLayout.CENTER, myNodeRender);
            add(BorderLayout.EAST, myShortcutPanel);
            myShortcutPanel.setBorder(JBUI.Borders.emptyRight(8));
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            setFont(tree.getFont());

            myNodeRender.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            myNodeRender.setFont(tree.getFont());

            Object data = TreeUtil.getUserObject(value);
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

            myShortcutPanel.removeAll();
            if (shortcuts != null && shortcuts.length > 0) {
                for (Shortcut shortcut : shortcuts) {
                    String shortcutText = KeymapUtil.getShortcutText(shortcut, myUseUnicodeCharactersForShortcutsGetter.getAsBoolean());

                    BorderLayoutPanel holder = new BorderLayoutPanel();
                    holder.withBorder(new RoundedLineBorder(JBColor.border(), 8));
                    holder.withBackground(JBColor.border());

                    JLabel label = new JLabel(shortcutText);
                    label.setOpaque(false);
                    holder.addToCenter(label);

                    myShortcutPanel.add(holder);
                }
            }

            return this;
        }
    }

    private static final Image CLOSE_ICON = PlatformIconGroup.nodesFolder();

    private final JTree myTree;
    private DefaultMutableTreeNode myRoot;
    private final JScrollPane myComponent;
    private Keymap myKeymap;
    private KeymapGroupImpl myMainGroup = new KeymapGroupImpl(LocalizeValue.empty());
    private boolean myShowBoundActions = Registry.is("keymap.show.alias.actions");

    private static final String ROOT = "ROOT";

    private String myFilter = null;
    private final DefaultTreeModel myModel;

    public ActionsTree(@Nonnull Disposable disposable) {
        this(disposable, ShortcutUtil::isUseUnicodeShortcuts);
    }

    public ActionsTree(@Nonnull Disposable disposable, @Nonnull BooleanSupplier useUnicodeCharactersForShortcutsGetter) {
        myRoot = new DefaultMutableTreeNode(ROOT);

        myModel = new DefaultTreeModel(myRoot);
        myTree = new Tree(new AsyncTreeModel(myModel, disposable));
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);

        myTree.setCellRenderer(new MyRenderer(useUnicodeCharactersForShortcutsGetter));

        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        myComponent = ScrollPaneFactory.createScrollPane(
            myTree,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
    }

    public void updateTree() {
        myTree.treeDidChange();
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
        if (selectionPath == null) {
            return null;
        }
        return ((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getUserObject();
    }

    @Nullable
    public String getSelectedActionId() {
        Object userObject = getSelectedObject();
        if (userObject instanceof String actionId) {
            return actionId;
        }
        if (userObject instanceof QuickList quickList) {
            return quickList.getActionId();
        }
        return null;
    }

    @Nullable
    public QuickList getSelectedQuickList() {
        return getSelectedObject() instanceof QuickList quickList ? quickList : null;
    }

    public void reset(Keymap keymap, QuickList[] allQuickLists) {
        reset(keymap, allQuickLists, myFilter, null);
    }

    public KeymapGroupImpl getMainGroup() {
        return myMainGroup;
    }

    public JTree getTree() {
        return myTree;
    }

    public void filter(String filter, QuickList[] currentQuickListIds) {
        myFilter = filter;
        reset(myKeymap, currentQuickListIds, filter, null);
    }

    private void reset(Keymap keymap, QuickList[] allQuickLists, String filter, @Nullable KeyboardShortcut shortcut) {
        myKeymap = keymap;

        PathsKeeper pathsKeeper = new PathsKeeper();
        pathsKeeper.storePaths();

        myRoot.removeAllChildren();

        ActionManager actionManager = ActionManager.getInstance();
        Project project = DataManager.getInstance().getDataContext(myComponent).getData(Project.KEY);
        KeymapGroupImpl mainGroup = ActionsTreeUtil.createMainGroup(
            project,
            myKeymap,
            allQuickLists,
            filter,
            true,
            filter != null && filter.length() > 0
                ? ActionsTreeUtil.isActionFiltered(filter, true)
                : shortcut != null ? ActionsTreeUtil.isActionFiltered(actionManager, myKeymap, shortcut) : null
        );
        if ((filter != null && filter.length() > 0 || shortcut != null) && mainGroup.initIds().isEmpty()) {
            mainGroup = ActionsTreeUtil.createMainGroup(
                project,
                myKeymap,
                allQuickLists,
                filter,
                false,
                filter != null && filter.length() > 0
                    ? ActionsTreeUtil.isActionFiltered(filter, false)
                    : ActionsTreeUtil.isActionFiltered(actionManager, myKeymap, shortcut)
            );
        }
        myRoot = ActionsTreeUtil.createNode(mainGroup);
        myMainGroup = mainGroup;
        myModel.setRoot(myRoot);
        myModel.nodeStructureChanged(myRoot);

        pathsKeeper.restorePaths();
    }

    public void filterTree(KeyboardShortcut keyboardShortcut, QuickList[] currentQuickListIds) {
        reset(myKeymap, currentQuickListIds, myFilter, keyboardShortcut);
    }

    private static boolean isActionChanged(String actionId, Keymap oldKeymap, Keymap newKeymap) {
        if (!newKeymap.canModify()) {
            return false;
        }

        Shortcut[] oldShortcuts = oldKeymap.getShortcuts(actionId);
        Shortcut[] newShortcuts = newKeymap.getShortcuts(actionId);
        return !Arrays.equals(oldShortcuts, newShortcuts);
    }

    private static boolean isGroupChanged(KeymapGroupImpl group, Keymap oldKeymap, Keymap newKeymap) {
        if (!newKeymap.canModify()) {
            return false;
        }

        List children = group.getChildren();
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
        JTree tree = myTree;

        String path = myMainGroup.getActionQualifiedPath(actionId);
        if (path == null) {
            return;
        }
        DefaultMutableTreeNode node = getNodeForPath(path);
        if (node == null) {
            return;
        }

        TreeUtil.selectInTree(node, true, tree);
    }

    @Nullable
    private DefaultMutableTreeNode getNodeForPath(String path) {
        Enumeration enumeration = ((DefaultMutableTreeNode) myTree.getModel().getRoot()).preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (Comparing.equal(getPath(node), path)) {
                return node;
            }
        }
        return null;
    }

    private List<DefaultMutableTreeNode> getNodesByPaths(List<String> paths) {
        List<DefaultMutableTreeNode> result = new ArrayList<>();
        Enumeration enumeration = ((DefaultMutableTreeNode) myTree.getModel().getRoot()).preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            String path = getPath(node);
            if (paths.contains(path)) {
                result.add(node);
            }
        }
        return result;
    }

    @Nullable
    private String getPath(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof String actionId) {
            if (node.getParent() instanceof DefaultMutableTreeNode defaultMutableTreeNode
                && defaultMutableTreeNode.getUserObject() instanceof KeymapGroupImpl keymapGroup) {
                return keymapGroup.getActionQualifiedPath(actionId);
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
        if (icon == null) {
            return Image.empty(Image.DEFAULT_ICON_SIZE);
        }
        return icon;
    }

    private class PathsKeeper {
        private List<String> myPathsToExpand;
        private List<String> mySelectionPaths;

        public void storePaths() {
            myPathsToExpand = new ArrayList<>();
            mySelectionPaths = new ArrayList<>();

            DefaultMutableTreeNode root = (DefaultMutableTreeNode) myTree.getModel().getRoot();

            TreePath path = new TreePath(root.getPath());
            if (myTree.isPathSelected(path)) {
                addPathToList(root, mySelectionPaths);
            }
            if (myTree.isExpanded(path) || root.getChildCount() == 0) {
                addPathToList(root, myPathsToExpand);
                _storePaths(root);
            }
        }

        private void addPathToList(DefaultMutableTreeNode root, List<String> list) {
            String path = getPath(root);
            if (!StringUtil.isEmpty(path)) {
                list.add(path);
            }
        }

        private void _storePaths(DefaultMutableTreeNode root) {
            List<TreeNode> childNodes = childrenToArray(root);
            for (Object childNode1 : childNodes) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childNode1;
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
            List<DefaultMutableTreeNode> nodesToExpand = getNodesByPaths(myPathsToExpand);
            for (DefaultMutableTreeNode node : nodesToExpand) {
                myTree.expandPath(new TreePath(node.getPath()));
            }

            if (myTree.getSelectionModel().getSelectionCount() == 0) {
                List<DefaultMutableTreeNode> nodesToSelect = getNodesByPaths(mySelectionPaths);
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

        private List<TreeNode> childrenToArray(DefaultMutableTreeNode node) {
            List<TreeNode> list = new ArrayList<>();
            for (int i = 0; i < node.getChildCount(); i++) {
                list.add(node.getChildAt(i));
            }
            return list;
        }
    }

    private class KeymapsRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(
            @Nonnull JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            boolean showIcons = UISettings.getInstance().SHOW_ICONS_IN_MENUS;
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
                    if (icon == null) {
                        icon = CLOSE_ICON;
                    }
                }
                else if (userObject instanceof String actionId) {
                    bound = myShowBoundActions && ((KeymapImpl) myKeymap).isActionBound(actionId);
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
                    icon = PlatformIconGroup.actionsQuicklist();
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
                    foreground = UIUtil.getTreeSelectionForeground(true);
                }
                else {
                    foreground = changed ? JBColor.BLUE : UIUtil.getTreeForeground();

                    if (bound) {
                        foreground = JBColor.MAGENTA;
                    }
                }
                SearchUtil.appendFragments(
                    myFilter,
                    text,
                    Font.PLAIN,
                    foreground,
                    selected ? UIUtil.getTreeSelectionBackground(true) : UIUtil.getTreeTextBackground(),
                    this
                );
            }
        }
    }
}
