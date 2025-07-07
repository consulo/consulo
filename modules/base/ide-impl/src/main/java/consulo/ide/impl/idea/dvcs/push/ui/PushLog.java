/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.push.ui;

import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.TextRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesTreeBrowser;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowser;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.EditSourceForDialogAction;
import consulo.ide.impl.idea.ui.AncestorListenerAdapter;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.log.Hash;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public class PushLog extends JPanel implements DataProvider {
    private static final String CONTEXT_MENU = "Vcs.Push.ContextMenu";
    private static final String START_EDITING = "startEditing";
    private final ChangesBrowser myChangesBrowser;
    private final CheckboxTree myTree;
    private final MyTreeCellRenderer myTreeCellRenderer;
    private final JScrollPane myScrollPane;
    private final VcsCommitInfoBalloon myBalloon;
    private boolean myShouldRepaint = false;
    private boolean mySyncStrategy;
    @Nullable
    private String mySyncRenderedText;
    private final boolean myAllowSyncStrategy;

    public PushLog(Project project, CheckedTreeNode root, boolean allowSyncStrategy) {
        myAllowSyncStrategy = allowSyncStrategy;
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        treeModel.nodeStructureChanged(root);
        AnAction quickDocAction = ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC);
        myTreeCellRenderer = new MyTreeCellRenderer();
        myTree = new CheckboxTree(myTreeCellRenderer, root) {
            @Override
            protected boolean shouldShowBusyIconIfNeeded() {
                return true;
            }

            @Override
            public boolean isPathEditable(TreePath path) {
                return isEditable() && path.getLastPathComponent() instanceof DefaultMutableTreeNode;
            }

            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                if (node instanceof EditableTreeNode editableTreeNode) {
                    editableTreeNode.fireOnSelectionChange(node.isChecked());
                }
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                TreePath path = myTree.getPathForLocation(event.getX(), event.getY());
                if (path == null) {
                    return "";
                }
                Object node = path.getLastPathComponent();
                if (node == null || !(node instanceof DefaultMutableTreeNode)) {
                    return "";
                }
                if (node instanceof TooltipNode tooltipNode) {
                    return KeymapUtil.createTooltipText(
                        tooltipNode.getTooltip() +
                            "<p style='font-style:italic;color:gray;'>Show commit details",
                        quickDocAction
                    ) + "</p>";
                }
                return "";
            }

            @Override
            public boolean stopEditing() {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                if (node instanceof EditableTreeNode) {
                    JComponent editedComponent = (JComponent)node.getUserObject();
                    InputVerifier verifier = editedComponent.getInputVerifier();
                    if (verifier != null && !verifier.verify(editedComponent)) {
                        return false;
                    }
                }
                boolean result = super.stopEditing();
                if (myShouldRepaint) {
                    refreshNode(root);
                }
                restoreSelection(node);
                return result;
            }

            @Override
            public void cancelEditing() {
                DefaultMutableTreeNode lastSelectedPathComponent = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                super.cancelEditing();
                if (myShouldRepaint) {
                    refreshNode(root);
                }
                restoreSelection(lastSelectedPathComponent);
            }
        };
        myTree.setUI(new MyTreeUi());
        myTree.setBorder(new EmptyBorder(2, 0, 0, 0));  //additional vertical indent
        myTree.setEditable(true);
        myTree.setHorizontalAutoScrollingEnabled(false);
        myTree.setShowsRootHandles(root.getChildCount() > 1);
        MyTreeCellEditor treeCellEditor = new MyTreeCellEditor();
        myTree.setCellEditor(treeCellEditor);
        treeCellEditor.addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                if (node != null && node instanceof EditableTreeNode treeNode) {
                    JComponent editedComponent = (JComponent)node.getUserObject();
                    InputVerifier verifier = editedComponent.getInputVerifier();
                    if (verifier != null && !verifier.verify(editedComponent)) {
                        // if invalid and interrupted, then revert
                        treeNode.fireOnCancel();
                    }
                    else if (mySyncStrategy) {
                        resetEditSync();
                        ContainerUtil.process(
                            getChildNodesByType(root, RepositoryNode.class, false),
                            node1 -> {
                                node1.fireOnChange();
                                return true;
                            }
                        );
                    }
                    else {
                        treeNode.fireOnChange();
                    }
                }
                myTree.firePropertyChange(PushLogTreeUtil.EDIT_MODE_PROP, true, false);
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                if (node != null && node instanceof EditableTreeNode editableTreeNode) {
                    editableTreeNode.fireOnCancel();
                }
                resetEditSync();
                myTree.firePropertyChange(PushLogTreeUtil.EDIT_MODE_PROP, true, false);
            }
        });
        // complete editing when interrupt
        myTree.setInvokesStopCellEditing(true);
        myTree.setRootVisible(false);
        TreeUtil.collapseAll(myTree, 1);
        VcsBranchEditorListener linkMouseListener = new VcsBranchEditorListener(myTreeCellRenderer);
        linkMouseListener.installOn(myTree);
        myBalloon = new VcsCommitInfoBalloon(myTree);
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        myTree.addTreeSelectionListener(e -> {
            updateChangesView();
            myBalloon.updateCommitDetails();
        });
        myTree.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                if (node instanceof RepositoryNode && myTree.isEditing()) {
                    //need to force repaint foreground  for non-focused editing node
                    myTree.getCellEditor().getTreeCellEditorComponent(
                        myTree,
                        node,
                        true,
                        false,
                        false,
                        myTree.getRowForPath(TreeUtil.getPathFromRoot(node))
                    );
                }
            }
        });
        myTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), START_EDITING);
        //override default tree behaviour.
        myTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "");
        MyShowCommitInfoAction showCommitInfoAction = new MyShowCommitInfoAction();
        showCommitInfoAction.registerCustomShortcutSet(quickDocAction.getShortcutSet(), myTree);

        ToolTipManager.sharedInstance().registerComponent(myTree);
        PopupHandler.installPopupHandler(myTree, VcsLogActionPlaces.POPUP_ACTION_GROUP, CONTEXT_MENU);

        myChangesBrowser = new ChangesBrowser(
            project,
            null,
            Collections.<Change>emptyList(),
            null,
            false,
            false,
            null,
            ChangesBrowser.MyUseCase.LOCAL_CHANGES,
            null
        );
        myChangesBrowser.getDiffAction().registerCustomShortcutSet(myChangesBrowser.getDiffAction().getShortcutSet(), myTree);
        EditSourceForDialogAction editSourceAction = new EditSourceForDialogAction(myChangesBrowser);
        editSourceAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), myChangesBrowser);
        myChangesBrowser.addToolbarAction(editSourceAction);
        setDefaultEmptyText();

        Splitter splitter = new Splitter(false, 0.7f);
        JComponent syncStrategyPanel = myAllowSyncStrategy ? createStrategyPanel() : null;
        myScrollPane = new JBScrollPane(myTree) {
            @Override
            public void layout() {
                super.layout();
                if (syncStrategyPanel != null) {
                    Rectangle bounds = this.getViewport().getBounds();
                    int height = bounds.height - syncStrategyPanel.getPreferredSize().height;
                    this.getViewport().setBounds(bounds.x, bounds.y, bounds.width, height);
                    syncStrategyPanel.setBounds(bounds.x, bounds.y + height, bounds.width,
                        syncStrategyPanel.getPreferredSize().height
                    );
                }
            }
        };
        if (syncStrategyPanel != null) {
            myScrollPane.setViewport(new MyTreeViewPort(myTree, syncStrategyPanel.getPreferredSize().height));
        }
        myScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        myScrollPane.setOpaque(false);
        if (syncStrategyPanel != null) {
            myScrollPane.add(syncStrategyPanel);
        }
        splitter.setFirstComponent(myScrollPane);
        splitter.setSecondComponent(myChangesBrowser);

        setLayout(new BorderLayout());
        add(splitter);
        myTree.setMinimumSize(new Dimension(200, myTree.getPreferredSize().height));
        myTree.setRowHeight(0);
    }

    private class MyShowCommitInfoAction extends DumbAwareAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myBalloon.showCommitDetails();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedCommitNodes().size() == 1);
        }
    }

    private void restoreSelection(@Nullable DefaultMutableTreeNode node) {
        if (node != null) {
            TreeUtil.selectNode(myTree, node);
        }
    }

    private JComponent createStrategyPanel() {
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(myTree.getBackground());
        LinkLabel<String> linkLabel = new LinkLabel<>("Edit all targets", null);
        linkLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        linkLabel.setListener(
            (aSource, aLinkData) -> {
                if (linkLabel.isEnabled()) {
                    startSyncEditing();
                }
            },
            null
        );
        myTree.addPropertyChangeListener(
            PushLogTreeUtil.EDIT_MODE_PROP,
            evt -> {
                Boolean editMode = (Boolean)evt.getNewValue();
                linkLabel.setEnabled(!editMode);
                linkLabel.setPaintUnderline(!editMode);
                linkLabel.repaint();
            }
        );
        labelPanel.add(linkLabel, BorderLayout.EAST);
        return labelPanel;
    }

    private void startSyncEditing() {
        mySyncStrategy = true;
        DefaultMutableTreeNode nodeToEdit = getFirstNodeToEdit();
        if (nodeToEdit != null) {
            myTree.startEditingAtPath(TreeUtil.getPathFromRoot(nodeToEdit));
        }
    }

    @Nonnull
    private static List<Change> collectAllChanges(@Nonnull List<CommitNode> commitNodes) {
        return CommittedChangesTreeBrowser.zipChanges(collectChanges(commitNodes));
    }

    @Nonnull
    private static List<CommitNode> collectSelectedCommitNodes(@Nonnull List<DefaultMutableTreeNode> selectedNodes) {
        List<CommitNode> nodes = new ArrayList<>();
        for (DefaultMutableTreeNode node : selectedNodes) {
            if (node instanceof RepositoryNode) {
                nodes.addAll(getChildNodesByType(node, CommitNode.class, true));
            }
            else if (node instanceof CommitNode commitNode && !nodes.contains(node)) {
                nodes.add(commitNode);
            }
        }
        return nodes;
    }

    @Nonnull
    private static List<Change> collectChanges(@Nonnull List<CommitNode> commitNodes) {
        List<Change> changes = new ArrayList<>();
        for (CommitNode node : commitNodes) {
            changes.addAll(node.getUserObject().getChanges());
        }
        return changes;
    }

    @Nonnull
    private static <T> List<T> getChildNodesByType(@Nonnull DefaultMutableTreeNode node, Class<T> type, boolean reverseOrder) {
        List<T> nodes = new ArrayList<>();
        if (node.getChildCount() < 1) {
            return nodes;
        }
        for (DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getFirstChild();
             childNode != null;
             childNode = (DefaultMutableTreeNode)node.getChildAfter(childNode)) {
            if (type.isInstance(childNode)) {
                @SuppressWarnings("unchecked")
                T nodeT = (T)childNode;
                if (reverseOrder) {
                    nodes.add(0, nodeT);
                }
                else {
                    nodes.add(nodeT);
                }
            }
        }
        return nodes;
    }

    @Nonnull
    private static List<Integer> getSortedRows(@Nonnull int[] rows) {
        List<Integer> sorted = new ArrayList<>();
        for (int row : rows) {
            sorted.add(row);
        }
        Collections.sort(sorted, Collections.reverseOrder());
        return sorted;
    }

    private void updateChangesView() {
        List<CommitNode> commitNodes = getSelectedCommitNodes();
        if (!commitNodes.isEmpty()) {
            myChangesBrowser.getViewer().setEmptyText("No differences");
        }
        else {
            setDefaultEmptyText();
        }
        myChangesBrowser.setChangesToDisplay(collectAllChanges(commitNodes));
    }

    private void setDefaultEmptyText() {
        myChangesBrowser.getViewer().setEmptyText("No commits selected");
    }

    // Make changes available for diff action; revisionNumber for create patch and copy revision number actions
    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (VcsDataKeys.CHANGES == dataId) {
            List<CommitNode> commitNodes = getSelectedCommitNodes();
            return ArrayUtil.toObjectArray(collectAllChanges(commitNodes), Change.class);
        }
        else if (VcsDataKeys.VCS_REVISION_NUMBERS == dataId) {
            List<CommitNode> commitNodes = getSelectedCommitNodes();
            return ArrayUtil.toObjectArray(
                ContainerUtil.map(
                    commitNodes,
                    (Function<CommitNode, VcsRevisionNumber>)commitNode -> {
                        Hash hash = commitNode.getUserObject().getId();
                        return new TextRevisionNumber(hash.asString(), hash.toShortString());
                    }
                ),
                VcsRevisionNumber.class
            );
        }
        return null;
    }

    @Nonnull
    private List<CommitNode> getSelectedCommitNodes() {
        int[] rows = myTree.getSelectionRows();
        if (rows != null && rows.length != 0) {
            List<DefaultMutableTreeNode> selectedNodes = getNodesForRows(getSortedRows(rows));
            return collectSelectedCommitNodes(selectedNodes);
        }
        return List.of();
    }

    @Nonnull
    private List<DefaultMutableTreeNode> getNodesForRows(@Nonnull List<Integer> rows) {
        List<DefaultMutableTreeNode> nodes = new ArrayList<>();
        for (Integer row : rows) {
            TreePath path = myTree.getPathForRow(row);
            Object pathComponent = path == null ? null : path.getLastPathComponent();
            if (pathComponent instanceof DefaultMutableTreeNode mutableTreeNode) {
                nodes.add(mutableTreeNode);
            }
        }
        return nodes;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0 && pressed) {
            if (myTree.isEditing()) {
                myTree.stopEditing();
            }
            else {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
                if (node != null) {
                    myTree.startEditingAtPath(TreeUtil.getPathFromRoot(node));
                }
            }
            return true;
        }
        if (myAllowSyncStrategy && e.getKeyCode() == KeyEvent.VK_F2 && e.getModifiers() == InputEvent.ALT_MASK && pressed) {
            startSyncEditing();
            return true;
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    @Nullable
    private DefaultMutableTreeNode getFirstNodeToEdit() {
        // start edit last selected component if editable
        if (myTree.getLastSelectedPathComponent() instanceof RepositoryNode repositoryNode) {
            RepositoryNode selectedNode = repositoryNode;
            if (selectedNode.isEditableNow()) {
                return selectedNode;
            }
        }
        List<RepositoryNode> repositoryNodes = getChildNodesByType(
            (DefaultMutableTreeNode)myTree.getModel().getRoot(),
            RepositoryNode.class,
            false
        );
        RepositoryNode editableNode = ContainerUtil.find(repositoryNodes, RepositoryNode::isEditableNow);
        if (editableNode != null) {
            TreeUtil.selectNode(myTree, editableNode);
        }
        return editableNode;
    }

    public JComponent getPreferredFocusedComponent() {
        return myTree;
    }

    @Nonnull
    public CheckboxTree getTree() {
        return myTree;
    }

    public void selectIfNothingSelected(@Nonnull TreeNode node) {
        if (myTree.isSelectionEmpty()) {
            myTree.setSelectionPath(TreeUtil.getPathFromRoot(node));
        }
    }

    public void setChildren(
        @Nonnull DefaultMutableTreeNode parentNode,
        @Nonnull Collection<? extends DefaultMutableTreeNode> childrenNodes
    ) {
        parentNode.removeAllChildren();
        for (DefaultMutableTreeNode child : childrenNodes) {
            parentNode.add(child);
        }
        if (!myTree.isEditing()) {
            refreshNode(parentNode);
            TreePath path = TreeUtil.getPathFromRoot(parentNode);
            if (myTree.getSelectionModel().isPathSelected(path)) {
                updateChangesView();
            }
        }
        else {
            myShouldRepaint = true;
        }
    }

    private void refreshNode(@Nonnull DefaultMutableTreeNode parentNode) {
        //todo should be optimized in case of start loading just edited node
        DefaultTreeModel model = ((DefaultTreeModel)myTree.getModel());
        model.nodeStructureChanged(parentNode);
        expandSelected(parentNode);
        myShouldRepaint = false;
    }

    private void expandSelected(@Nonnull DefaultMutableTreeNode node) {
        if (node.getChildCount() <= 0) {
            return;
        }
        if (node instanceof RepositoryNode) {
            TreePath path = TreeUtil.getPathFromRoot(node);
            myTree.expandPath(path);
            return;
        }
        for (DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getFirstChild();
             childNode != null;
             childNode = (DefaultMutableTreeNode)node.getChildAfter(childNode)) {
            if (!(childNode instanceof RepositoryNode)) {
                return;
            }
            TreePath path = TreeUtil.getPathFromRoot(childNode);
            if (((RepositoryNode)childNode).isChecked()) {
                myTree.expandPath(path);
            }
        }
    }

    private void setSyncText(String value) {
        mySyncRenderedText = value;
    }

    public void fireEditorUpdated(@Nonnull String currentText) {
        if (mySyncStrategy) {
            //update ui model
            List<RepositoryNode> repositoryNodes =
                getChildNodesByType((DefaultMutableTreeNode)myTree.getModel().getRoot(), RepositoryNode.class, false);
            for (RepositoryNode node : repositoryNodes) {
                if (node.isEditableNow()) {
                    node.forceUpdateUiModelWithTypedText(currentText);
                }
            }
            setSyncText(currentText);
            myTree.repaint();
        }
    }

    private void resetEditSync() {
        if (mySyncStrategy) {
            mySyncStrategy = false;
            mySyncRenderedText = null;
        }
    }

    private class MyTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        @Override
        public void customizeRenderer(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (!(value instanceof DefaultMutableTreeNode)) {
                return;
            }
            myCheckbox.setBorder(null); //checkBox may have no border by default, but insets are not null,
            // it depends on LaF, OS and isItRenderedPane, see consulo.ide.impl.idea.ide.ui.laf.darcula.ui.DarculaCheckBoxBorder.
            // null border works as expected always.
            if (value instanceof RepositoryNode valueNode) {
                //todo simplify, remove instance of
                myCheckbox.setVisible(valueNode.isCheckboxVisible());
                if (valueNode.isChecked() && valueNode.isLoading()) {
                    myCheckbox.setState(ThreeStateCheckBox.State.DONT_CARE);
                }
                else {
                    myCheckbox.setSelected(valueNode.isChecked());
                }
            }
            Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
            ColoredTreeCellRenderer renderer = getTextRenderer();
            if (value instanceof CustomRenderedTreeNode customRenderedTreeNode) {
                if (tree.isEditing() && mySyncStrategy && value instanceof RepositoryNode repositoryNode) {
                    //sync rendering all editable fields
                    repositoryNode.render(renderer, mySyncRenderedText);
                }
                else {
                    customRenderedTreeNode.render(renderer);
                }
            }
            else {
                renderer.append(userObject == null ? "" : userObject.toString());
            }
        }
    }

    private class MyTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {
        private RepositoryWithBranchPanel myValue;

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            RepositoryWithBranchPanel panel = (RepositoryWithBranchPanel)((DefaultMutableTreeNode)value).getUserObject();
            myValue = panel;
            myTree.firePropertyChange(PushLogTreeUtil.EDIT_MODE_PROP, false, true);
            return panel.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row, true);
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent me) {
                TreePath path = myTree.getClosestPathForLocation(me.getX(), me.getY());
                int row = myTree.getRowForLocation(me.getX(), me.getY());
                myTree.getCellRenderer()
                    .getTreeCellRendererComponent(myTree, path.getLastPathComponent(), false, false, true, row, true);
                Object tag = me.getClickCount() >= 1 ? PushLogTreeUtil.getTagAtForRenderer(myTreeCellRenderer, me) : null;
                return tag instanceof VcsEditableComponent;
            }
            //if keyboard event - then anEvent will be null =( See BasicTreeUi
            TreePath treePath = myTree.getAnchorSelectionPath();
            //there is no selection path if we start editing during initial validation//
            if (treePath == null) {
                return true;
            }
            Object treeNode = treePath.getLastPathComponent();
            return treeNode instanceof EditableTreeNode editableTreeNode && editableTreeNode.isEditableNow();
        }

        @Override
        public Object getCellEditorValue() {
            return myValue;
        }
    }

    private class MyTreeUi extends WideSelectionTreeUI {
        private final ComponentListener myTreeSizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // invalidate, revalidate etc may have no 'size' effects, you need to manually invalidateSizes before.
                updateSizes();
            }
        };

        private final AncestorListener myTreeAncestorListener = new AncestorListenerAdapter() {
            @Override
            public void ancestorMoved(AncestorEvent event) {
                super.ancestorMoved(event);
                updateSizes();
            }
        };

        private void updateSizes() {
            treeState.invalidateSizes();
            tree.repaint();
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            tree.addComponentListener(myTreeSizeListener);
            tree.addAncestorListener(myTreeAncestorListener);
        }


        @Override
        protected void uninstallListeners() {
            tree.removeComponentListener(myTreeSizeListener);
            tree.removeAncestorListener(myTreeAncestorListener);
            super.uninstallListeners();
        }

        @Override
        protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
            return new NodeDimensionsHandler() {
                @Override
                public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle size) {
                    Rectangle dimensions = super.getNodeDimensions(value, row, depth, expanded, size);
                    dimensions.width = myScrollPane != null
                        ? Math.max(myScrollPane.getViewport().getWidth() - getRowX(row, depth), dimensions.width)
                        : Math.max(myTree.getMinimumSize().width, dimensions.width);
                    return dimensions;
                }
            };
        }
    }

    private static class MyTreeViewPort extends JBViewport {
        final int myHeightToReduce;

        public MyTreeViewPort(@Nullable Component view, int heightToReduce) {
            super();
            setView(view);
            myHeightToReduce = heightToReduce;
        }

        @Override
        public Dimension getExtentSize() {
            Dimension defaultSize = super.getExtentSize();
            return new Dimension(defaultSize.width, defaultSize.height - myHeightToReduce);
        }
    }
}
