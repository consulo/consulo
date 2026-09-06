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
import consulo.ui.ex.action.QuickList;
import consulo.ui.ex.impl.internal.keymap.KeymapImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public class ActionsTree {
    private static final Logger LOG = Logger.getInstance(ActionsTree.class);

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

            Object data = unwrap(value);
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
    private final JScrollPane myComponent;
    private Keymap myKeymap;
    private KeymapGroupImpl myMainGroup = new KeymapGroupImpl(LocalizeValue.empty());
    private boolean myShowBoundActions = Registry.is("keymap.show.alias.actions");

    private static final String PATH_SEPARATOR = " | ";

    private String myFilter = null;
    private final KeymapTreeStructure myStructure;
    private final StructureTreeModel<KeymapTreeStructure> myStructureTreeModel;

    public ActionsTree(Disposable disposable) {
        this(disposable, ShortcutUtil::isUseUnicodeShortcuts);
    }

    public ActionsTree(Disposable disposable, BooleanSupplier useUnicodeCharactersForShortcutsGetter) {
        myStructure = new KeymapTreeStructure(myMainGroup);
        myStructureTreeModel = new StructureTreeModel<>(myStructure, disposable);
        myTree = new Tree(new AsyncTreeModel(myStructureTreeModel, disposable));
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

    private static @Nullable KeymapTreeElement asElement(@Nullable Object node) {
        return TreeUtil.getUserObject(node) instanceof NodeDescriptor<?> descriptor
            && descriptor.getElement() instanceof KeymapTreeElement element ? element : null;
    }

    public static @Nullable Object unwrap(@Nullable Object node) {
        KeymapTreeElement element = asElement(node);
        return element == null ? null : element.getValue();
    }

    private @Nullable Object getSelectedObject() {
        TreePath selectionPath = myTree.getSelectionPath();
        return selectionPath == null ? null : unwrap(selectionPath.getLastPathComponent());
    }

    public @Nullable String getSelectedActionId() {
        Object userObject = getSelectedObject();
        if (userObject instanceof String actionId) {
            return actionId;
        }
        if (userObject instanceof QuickList quickList) {
            return quickList.getActionId();
        }
        return null;
    }

    public @Nullable QuickList getSelectedQuickList() {
        return getSelectedObject() instanceof QuickList quickList ? quickList : null;
    }

    public CompletableFuture<?> reset(Keymap keymap, QuickList[] allQuickLists) {
        return reset(keymap, allQuickLists, myFilter, null);
    }

    public KeymapGroupImpl getMainGroup() {
        return myMainGroup;
    }

    public JTree getTree() {
        return myTree;
    }

    public CompletableFuture<?> filter(String filter, QuickList[] currentQuickListIds) {
        myFilter = filter;
        return reset(myKeymap, currentQuickListIds, filter, null);
    }

    private CompletableFuture<?> reset(
        Keymap keymap,
        QuickList[] allQuickLists,
        String filter,
        @Nullable KeyboardShortcut shortcut
    ) {
        myKeymap = keymap;

        PathsKeeper pathsKeeper = new PathsKeeper();
        pathsKeeper.storePaths();

        ActionManager actionManager = ActionManager.getInstance();
        Project project = DataManager.getInstance().getDataContext(myComponent).getData(Project.KEY);
        UIAccess uiAccess = UIAccess.current();

        return ActionsTreeUtil.createMainGroupAsync(
            project,
            myKeymap,
            allQuickLists,
            filter,
            true,
            filter != null && filter.length() > 0
                ? ActionsTreeUtil.isActionFiltered(filter, true)
                : shortcut != null ? ActionsTreeUtil.isActionFiltered(actionManager, myKeymap, shortcut) : null
        ).thenCompose(mainGroup -> {
            if ((filter != null && filter.length() > 0 || shortcut != null) && mainGroup.initIds().isEmpty()) {
                return ActionsTreeUtil.createMainGroupAsync(
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
            return CompletableFuture.completedFuture(mainGroup);
        }).thenCompose(mainGroup -> uiAccess.giveAsync(() -> {
            myMainGroup = mainGroup;
            myStructure.setRootGroup(mainGroup);
            myStructureTreeModel.invalidate()
                .onSuccess(ignored -> uiAccess.giveAsync(pathsKeeper::restorePaths));
        })).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                LOG.error("Failed to build the keymap action tree", throwable);
            }
        });
    }

    public CompletableFuture<?> filterTree(KeyboardShortcut keyboardShortcut, QuickList[] currentQuickListIds) {
        return reset(myKeymap, currentQuickListIds, myFilter, keyboardShortcut);
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

        for (Object child : group.getChildren()) {
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
        String path = myMainGroup.getActionQualifiedPath(actionId);
        if (path == null) {
            return;
        }
        TreeUtil.promiseSelect(myTree, pathVisitor(path));
    }

    private TreeVisitor pathVisitor(String target) {
        return treePath -> {
            String candidate = getPath(treePath.getLastPathComponent());
            if (StringUtil.isEmpty(candidate)) {
                return TreeVisitor.Action.CONTINUE;
            }
            if (target.equals(candidate)) {
                return TreeVisitor.Action.INTERRUPT;
            }
            return target.startsWith(candidate + PATH_SEPARATOR)
                ? TreeVisitor.Action.CONTINUE
                : TreeVisitor.Action.SKIP_CHILDREN;
        };
    }

    private @Nullable String getPath(@Nullable Object node) {
        KeymapTreeElement element = asElement(node);
        if (element == null) {
            return null;
        }
        Object value = element.getValue();
        if (value instanceof String actionId) {
            KeymapTreeElement parent = element.getParent();
            if (parent != null && parent.getValue() instanceof KeymapGroupImpl keymapGroup) {
                return keymapGroup.getActionQualifiedPath(actionId);
            }
            return myMainGroup.getActionQualifiedPath(actionId);
        }
        if (value instanceof KeymapGroupImpl keymapGroup) {
            return keymapGroup.getQualifiedPath();
        }
        if (value instanceof QuickList quickList) {
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
        private List<String> myPathsToExpand = new ArrayList<>();
        private List<String> mySelectionPaths = new ArrayList<>();

        public void storePaths() {
            myPathsToExpand = new ArrayList<>();
            mySelectionPaths = new ArrayList<>();

            Object root = myTree.getModel().getRoot();
            if (root == null) {
                return;
            }

            Enumeration<TreePath> expanded = myTree.getExpandedDescendants(new TreePath(root));
            if (expanded != null) {
                while (expanded.hasMoreElements()) {
                    addPathToList(expanded.nextElement(), myPathsToExpand);
                }
            }

            TreePath[] selection = myTree.getSelectionPaths();
            if (selection != null) {
                for (TreePath treePath : selection) {
                    addPathToList(treePath, mySelectionPaths);
                }
            }
        }

        private void addPathToList(TreePath treePath, List<String> list) {
            String path = getPath(treePath.getLastPathComponent());
            if (!StringUtil.isEmpty(path)) {
                list.add(path);
            }
        }

        public void restorePaths() {
            if (!myPathsToExpand.isEmpty()) {
                TreeUtil.promiseExpand(myTree, myPathsToExpand.stream().map(ActionsTree.this::pathVisitor));
            }

            if (myTree.getSelectionModel().getSelectionCount() == 0) {
                if (mySelectionPaths.isEmpty()) {
                    TreeUtil.promiseSelectFirst(myTree);
                }
                else {
                    TreeUtil.promiseSelect(myTree, mySelectionPaths.stream().map(ActionsTree.this::pathVisitor));
                }
            }
        }
    }

    private class KeymapsRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (!(value instanceof DefaultMutableTreeNode defaultMutableTreeNode)) {
                return;
            }

            boolean showIcons = UISettings.getInstance().SHOW_ICONS_IN_MENUS;
            Keymap originalKeymap = myKeymap != null ? myKeymap.getParent() : null;
            Image icon = null;
            String text;
            boolean bound = false;
            Object userObject = unwrap(defaultMutableTreeNode);

            boolean changed;
            if (userObject == null) {
                return;
            }
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
                    if (StringUtil.isEmpty(text)) { //fill dynamic presentation gaps
                        text = actionId;
                    }
                    icon = action.getTemplatePresentation().getIcon();
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
