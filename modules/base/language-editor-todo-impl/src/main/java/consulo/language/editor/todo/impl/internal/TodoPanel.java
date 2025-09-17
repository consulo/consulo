// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.todo.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.application.ui.util.TodoPanelSettings;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.language.editor.todo.TodoConfiguration;
import consulo.language.editor.todo.TodoFilter;
import consulo.language.editor.todo.impl.internal.action.SetTodoFilterAction;
import consulo.language.editor.todo.impl.internal.localize.LanguageTodoLocalize;
import consulo.language.editor.todo.impl.internal.node.*;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNavigationSupport;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.action.NextOccurenceToolbarAction;
import consulo.project.ui.action.PreviousOccurenceToolbarAction;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.usage.UsageInfo;
import consulo.usage.UsagePreviewPanel;
import consulo.usage.UsagePreviewPanelFactory;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class TodoPanel extends SimpleToolWindowPanel implements OccurenceNavigator, DataProvider, Disposable {
    public abstract static class GroupByOptionAction extends ToggleAction {
        private final Function<TodoPanel, Boolean> myGetter;
        @RequiredUIAccess
        private final BiConsumer<TodoPanel, Boolean> mySetter;

        public GroupByOptionAction(
            LocalizeValue text,
            Image icon,
            Function<TodoPanel, Boolean> getter,
            @RequiredUIAccess BiConsumer<TodoPanel, Boolean> setter
        ) {
            super(text, LocalizeValue.empty(), icon);
            myGetter = getter;
            mySetter = setter;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            if (!e.hasData(TODO_PANEL_DATA_KEY)) {
                e.getPresentation().setEnabled(false);
            }
            super.update(e);
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
            return todoPanel != null && myGetter.apply(todoPanel);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
            if (todoPanel != null) {
                mySetter.accept(todoPanel, state);
            }
        }
    }

    protected static final Logger LOG = Logger.getInstance(TodoPanel.class);

    @Nonnull
    protected Project myProject;
    private final TodoPanelSettings mySettings;
    private final boolean myCurrentFileMode;
    private final Content myContent;

    private final Tree myTree;
    private final MyTreeExpander myTreeExpander;
    private final MyOccurenceNavigator myOccurenceNavigator;
    protected final TodoTreeBuilder myTodoTreeBuilder;
    private MyVisibilityWatcher myVisibilityWatcher;
    private JComponent myUsagePreviewPanelComponent;
    private UsagePreviewPanel myUsagePreviewPanel;
    private MyAutoScrollToSourceHandler myAutoScrollToSourceHandler;

    public static final Key<TodoPanel> TODO_PANEL_DATA_KEY = Key.create("TodoPanel");

    /**
     * @param currentFileMode if {@code true} then view doesn't have "KeymapGroupImpl By Packages" and "Flatten Packages" actions.
     */
    @RequiredUIAccess
    TodoPanel(@Nonnull Project project, TodoPanelSettings settings, boolean currentFileMode, Content content) {
        super(false, true);

        myProject = project;
        mySettings = settings;
        myCurrentFileMode = currentFileMode;
        myContent = content;

        DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
        myTree = new Tree(model);
        myTreeExpander = new MyTreeExpander();
        myOccurenceNavigator = new MyOccurenceNavigator();
        initUI();
        myTodoTreeBuilder = setupTreeStructure();
        updateTodoFilter();
        myTodoTreeBuilder.setShowPackages(mySettings.arePackagesShown);
        myTodoTreeBuilder.setShowModules(mySettings.areModulesShown);
        myTodoTreeBuilder.setFlattenPackages(mySettings.areFlattenPackages);

        myVisibilityWatcher = new MyVisibilityWatcher();
        myVisibilityWatcher.install(this);
    }

    public boolean isModulesShown() {
        return mySettings.areModulesShown;
    }

    @RequiredUIAccess
    public void setModulesShown(boolean state) {
        mySettings.areModulesShown = state;
        myTodoTreeBuilder.setShowModules(state);
    }

    public boolean isPackagesShown() {
        return mySettings.arePackagesShown;
    }

    @RequiredUIAccess
    public void setPackagesShown(boolean state) {
        mySettings.arePackagesShown = state;
        myTodoTreeBuilder.setShowPackages(state);
    }

    public boolean isFlattenPackages() {
        return mySettings.areFlattenPackages;
    }

    @RequiredUIAccess
    public void setFlattenPackages(boolean state) {
        mySettings.areFlattenPackages = state;
        myTodoTreeBuilder.setFlattenPackages(state);
    }

    private TodoTreeBuilder setupTreeStructure() {
        TodoTreeBuilder todoTreeBuilder = createTreeBuilder(myTree, myProject);
        TodoTreeStructure structure = todoTreeBuilder.getTodoTreeStructure();
        StructureTreeModel structureTreeModel = new StructureTreeModel(structure, TodoTreeBuilder.NODE_DESCRIPTOR_COMPARATOR, this);
        AsyncTreeModel asyncTreeModel = new AsyncTreeModel(structureTreeModel, myProject);
        myTree.setModel(asyncTreeModel);
        asyncTreeModel.addTreeModelListener(new MyExpandListener(todoTreeBuilder));
        todoTreeBuilder.setModel(structureTreeModel);
        Object selectableElement = structure.getFirstSelectableElement();
        if (selectableElement != null) {
            todoTreeBuilder.select(selectableElement);
        }
        return todoTreeBuilder;
    }

    private class MyExpandListener extends TreeModelAdapter {
        private final TodoTreeBuilder myBuilder;

        MyExpandListener(TodoTreeBuilder builder) {
            myBuilder = builder;
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            TreePath parentPath = e.getTreePath();
            if (parentPath == null || parentPath.getPathCount() > 2) {
                return;
            }
            Object[] children = e.getChildren();
            for (Object o : children) {
                NodeDescriptor descriptor = TreeUtil.getUserObject(NodeDescriptor.class, o);
                if (descriptor != null && myBuilder.isAutoExpandNode(descriptor)) {
                    myBuilder.myProject.getApplication().invokeLater(
                        () -> {
                            if (myTree.isVisible(parentPath) && myTree.isExpanded(parentPath)) {
                                myTree.expandPath(parentPath.pathByAddingChild(o));
                            }
                        },
                        myBuilder.myProject.getDisposed()
                    );
                }
            }
        }
    }

    protected abstract TodoTreeBuilder createTreeBuilder(JTree tree, Project project);

    private void initUI() {
        myTree.setShowsRootHandles(true);
        myTree.setRootVisible(false);
        myTree.setRowHeight(0); // enable variable-height rows
        myTree.setCellRenderer(new TodoCompositeRenderer());
        EditSourceOnDoubleClickHandler.install(myTree);
        new TreeSpeedSearch(myTree);

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
        group.addSeparator();
        group.add(CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, this));
        group.add(CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, this));
        group.addSeparator();
        group.add(ActionManager.getInstance().getAction(IdeActions.GROUP_VERSION_CONTROLS));
        PopupHandler.installPopupHandler(myTree, group, ActionPlaces.TODO_VIEW_POPUP, ActionManager.getInstance());

        myTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!e.isConsumed() && KeyEvent.VK_ENTER == e.getKeyCode()) {
                    TreePath path = myTree.getSelectionPath();
                    if (path == null) {
                        return;
                    }
                    Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (!((userObject instanceof NodeDescriptor nodeDescriptor ? nodeDescriptor : null) instanceof TodoItemNode)) {
                        return;
                    }
                    OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(TodoPanel.this), false);
                }
            }
        });

        UsagePreviewPanelFactory panelFactory = UsagePreviewPanelFactory.getInstance();

        myUsagePreviewPanel = panelFactory.createPreviewPanel(
            myProject,
            panelFactory.createEmpUsageViewPresentation()
        );
        myUsagePreviewPanelComponent = myUsagePreviewPanel.createComponent();
        Disposer.register(this, myUsagePreviewPanel);
        myUsagePreviewPanelComponent.setVisible(mySettings.showPreview);

        setContent(createCenterComponent());

        myTree.getSelectionModel().addTreeSelectionListener(e -> myProject.getApplication().invokeLater(
            () -> {
                if (myUsagePreviewPanelComponent.isVisible()) {
                    updatePreviewPanel();
                }
            },
            ModalityState.nonModal(),
            myProject.getDisposed()
        ));

        myAutoScrollToSourceHandler = new MyAutoScrollToSourceHandler();
        myAutoScrollToSourceHandler.install(myTree);

        // Create tool bars and register custom shortcuts

        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        toolbarGroup.add(new PreviousOccurenceToolbarAction(myOccurenceNavigator));
        toolbarGroup.add(new NextOccurenceToolbarAction(myOccurenceNavigator));
        toolbarGroup.add(new SetTodoFilterAction(myProject, mySettings, this::setTodoFilter));
        toolbarGroup.add(createAutoScrollToSourceAction());

        if (!myCurrentFileMode) {
            DefaultActionGroup groupBy = createGroupByActionGroup();
            toolbarGroup.add(groupBy);
        }

        toolbarGroup.add(new MyPreviewAction());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, toolbarGroup, false);
        toolbar.setTargetComponent(this);

        setToolbar(toolbar.getComponent());
    }

    @Nonnull
    protected DefaultActionGroup createGroupByActionGroup() {
        ActionManager actionManager = ActionManager.getInstance();
        return (DefaultActionGroup) actionManager.getAction("TodoViewGroupByGroup");
    }

    protected AnAction createAutoScrollToSourceAction() {
        return myAutoScrollToSourceHandler.createToggleAction();
    }

    protected JComponent createCenterComponent() {
        Splitter splitter = new OnePixelSplitter(false);
        splitter.setSecondComponent(myUsagePreviewPanelComponent);
        splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
        return splitter;
    }

    @RequiredReadAction
    private void updatePreviewPanel() {
        if (myProject.isDisposed()) {
            return;
        }
        List<UsageInfo> infos = new ArrayList<>();
        TreePath path = myTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof NodeDescriptor nodeDescriptor) {
                Object element = nodeDescriptor.getElement();
                TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
                if (pointer != null) {
                    SmartTodoItemPointer value = pointer.getValue();
                    Document document = value.getDocument();
                    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
                    RangeMarker rangeMarker = value.getRangeMarker();
                    if (psiFile != null) {
                        infos.add(new UsageInfo(psiFile, rangeMarker.getStartOffset(), rangeMarker.getEndOffset()));
                        for (RangeMarker additionalMarker : value.getAdditionalRangeMarkers()) {
                            if (additionalMarker.isValid()) {
                                infos.add(new UsageInfo(psiFile, additionalMarker.getStartOffset(), additionalMarker.getEndOffset()));
                            }
                        }
                    }
                }
            }
        }
        myUsagePreviewPanel.updateLayout(infos.isEmpty() ? null : infos);
    }

    @Override
    public void dispose() {
        if (myVisibilityWatcher != null) {
            myVisibilityWatcher.deinstall(this);
            myVisibilityWatcher = null;
        }
    }

    @RequiredUIAccess
    void rebuildCache() {
        myTodoTreeBuilder.rebuildCache();
    }

    @RequiredUIAccess
    void rebuildCache(@Nonnull Set<VirtualFile> files) {
        myTodoTreeBuilder.rebuildCache(files);
    }

    /**
     * Immediately updates tree.
     */
    @RequiredUIAccess
    void updateTree() {
        myTodoTreeBuilder.updateTree();
    }

    /**
     * Updates current filter. If previously set filter was removed then empty filter is set.
     *
     * @see TodoTreeBuilder#setTodoFilter
     */
    @RequiredUIAccess
    void updateTodoFilter() {
        TodoFilter filter = TodoConfiguration.getInstance().getTodoFilter(mySettings.todoFilterName);
        setTodoFilter(filter);
    }

    /**
     * Sets specified {@code TodoFilter}. The method also updates window's title.
     *
     * @see TodoTreeBuilder#setTodoFilter
     */
    @RequiredUIAccess
    private void setTodoFilter(TodoFilter filter) {
        // Clear name of current filter if it was removed from configuration.
        String filterName = filter != null ? filter.getName() : null;
        mySettings.todoFilterName = filterName;
        // Update filter
        myTodoTreeBuilder.setTodoFilter(filter);
        // Update content's title
        myContent.setDescription(filterName);
    }

    /**
     * @return list of all selected virtual files.
     */
    @Nullable
    protected PsiFile getSelectedFile() {
        TreePath path = myTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        LOG.assertTrue(node != null);
        if (node.getUserObject() == null) {
            return null;
        }
        return TodoTreeBuilder.getFileForNode(node);
    }

    protected void setDisplayName(String tabName) {
        myContent.setDisplayName(tabName);
    }

    @Nullable
    private PsiElement getSelectedElement() {
        if (myTree == null) {
            return null;
        }
        TreePath path = myTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        PsiElement selectedElement = TodoTreeHelper.getSelectedElement(userObject);
        if (selectedElement != null) {
            return selectedElement;
        }
        return getSelectedFile();
    }

    @Override
    public Object getData(@Nonnull Key dataId) {
        if (Navigatable.KEY == dataId) {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (!(node.getUserObject() instanceof NodeDescriptor nodeDescriptor)) {
                return null;
            }
            Object element = nodeDescriptor.getElement();
            if (!(element instanceof TodoFileNode || element instanceof TodoItemNode)) { // allow user to use F4 only on files an TODOs
                return null;
            }
            TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
            if (pointer != null) {
                return PsiNavigationSupport.getInstance().createNavigatable(
                    myProject,
                    pointer.getValue().getTodoItem().getFile().getVirtualFile(),
                    pointer.getValue().getRangeMarker().getStartOffset()
                );
            }
            else {
                return null;
            }
        }
        else if (VirtualFile.KEY == dataId) {
            PsiFile file = getSelectedFile();
            return file != null ? file.getVirtualFile() : null;
        }
        else if (PsiElement.KEY == dataId) {
            return getSelectedElement();
        }
        else if (VirtualFile.KEY_OF_ARRAY == dataId) {
            PsiFile file = getSelectedFile();
            if (file != null) {
                return new VirtualFile[]{file.getVirtualFile()};
            }
            else {
                return VirtualFile.EMPTY_ARRAY;
            }
        }
        else if (HelpManager.HELP_ID == dataId) {
            //noinspection HardCodedStringLiteral
            return "find.todoList";
        }
        else if (TODO_PANEL_DATA_KEY == dataId) {
            return this;
        }
        return super.getData(dataId);
    }

    @Nullable
    @Override
    public OccurenceInfo goPreviousOccurence() {
        return myOccurenceNavigator.goPreviousOccurence();
    }

    @Nonnull
    @Override
    public String getNextOccurenceActionName() {
        return myOccurenceNavigator.getNextOccurenceActionName();
    }

    @Nullable
    @Override
    public OccurenceInfo goNextOccurence() {
        return myOccurenceNavigator.goNextOccurence();
    }

    @Override
    public boolean hasNextOccurence() {
        return myOccurenceNavigator.hasNextOccurence();
    }

    @Nonnull
    @Override
    public String getPreviousOccurenceActionName() {
        return myOccurenceNavigator.getPreviousOccurenceActionName();
    }

    @Override
    public boolean hasPreviousOccurence() {
        return myOccurenceNavigator.hasPreviousOccurence();
    }

    protected void rebuildWithAlarm(Alarm alarm) {
        alarm.cancelAllRequests();
        alarm.addRequest(() -> {
            Set<VirtualFile> files = new HashSet<>();
            DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
                if (myTodoTreeBuilder.isDisposed()) {
                    return;
                }
                myTodoTreeBuilder.collectFiles(virtualFile -> {
                    files.add(virtualFile);
                    return true;
                });
                Runnable runnable = () -> {
                    if (myTodoTreeBuilder.isDisposed()) {
                        return;
                    }
                    myTodoTreeBuilder.rebuildCache(files);
                    updateTree();
                };
                myProject.getApplication().invokeLater(runnable);
            });
        }, 300);
    }

    TreeExpander getTreeExpander() {
        return myTreeExpander;
    }

    private final class MyTreeExpander implements TreeExpander {
        @Override
        public boolean canCollapse() {
            return true;
        }

        @Override
        public boolean canExpand() {
            return true;
        }

        @Override
        public void collapseAll() {
            TreeUtil.collapseAll(myTree, 0);
        }

        @Override
        public void expandAll() {
            TreeUtil.expandAll(myTree);
        }
    }

    /**
     * Provides support for "auto scroll to source" functionality
     */
    private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
        MyAutoScrollToSourceHandler() {
        }

        @Override
        protected boolean isAutoScrollMode() {
            return mySettings.isAutoScrollToSource;
        }

        @Override
        protected void setAutoScrollMode(boolean state) {
            mySettings.isAutoScrollToSource = state;
        }
    }

    /**
     * Provides support for "Ctrl+Alt+Up/Down" navigation.
     */
    private final class MyOccurenceNavigator implements OccurenceNavigator {
        @Override
        public boolean hasNextOccurence() {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return false;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (userObject == null) {
                return false;
            }
            if (userObject instanceof NodeDescriptor nodeDescriptor && nodeDescriptor.getElement() instanceof TodoItemNode) {
                return myTree.getRowCount() != myTree.getRowForPath(path) + 1;
            }
            else {
                return node.getChildCount() > 0;
            }
        }

        @Override
        public boolean hasPreviousOccurence() {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return false;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            return node.getUserObject() instanceof NodeDescriptor && !isFirst(node);
        }

        private boolean isFirst(TreeNode node) {
            TreeNode parent = node.getParent();
            return parent == null || parent.getIndex(node) == 0 && isFirst(parent);
        }

        @Override
        @Nullable
        public OccurenceInfo goNextOccurence() {
            return goToPointer(getNextPointer());
        }

        @Override
        @Nullable
        public OccurenceInfo goPreviousOccurence() {
            return goToPointer(getPreviousPointer());
        }

        @Nonnull
        @Override
        public String getNextOccurenceActionName() {
            return LanguageTodoLocalize.actionNextTodo().get();
        }

        @Nonnull
        @Override
        public String getPreviousOccurenceActionName() {
            return LanguageTodoLocalize.actionPreviousTodo().get();
        }

        @Nullable
        private OccurenceInfo goToPointer(TodoItemNode pointer) {
            if (pointer == null) {
                return null;
            }
            myTodoTreeBuilder.select(pointer);
            return new OccurenceInfo(
                PsiNavigationSupport.getInstance().createNavigatable(
                    myProject,
                    pointer.getValue().getTodoItem().getFile().getVirtualFile(),
                    pointer.getValue().getRangeMarker().getStartOffset()
                ),
                -1,
                -1
            );
        }

        @Nullable
        private TodoItemNode getNextPointer() {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (!(node.getUserObject() instanceof NodeDescriptor nodeDescriptor)) {
                return null;
            }
            Object element = nodeDescriptor.getElement();
            return element instanceof TodoItemNode todoItemNode
                ? myTodoTreeBuilder.getNextPointer(todoItemNode)
                : myTodoTreeBuilder.getFirstPointerForElement(element);
        }

        @Nullable
        private TodoItemNode getPreviousPointer() {
            TreePath path = myTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (!(node.getUserObject() instanceof NodeDescriptor nodeDescriptor)) {
                return null;
            }
            Object element = nodeDescriptor.getElement();
            if (element instanceof TodoItemNode todoItemNode) {
                return myTodoTreeBuilder.getPreviousPointer(todoItemNode);
            }
            else {
                Object sibling = myTodoTreeBuilder.getPreviousSibling(element);
                if (sibling == null) {
                    return null;
                }
                return myTodoTreeBuilder.getLastPointerForElement(sibling);
            }
        }
    }

    private final class MyVisibilityWatcher extends VisibilityWatcher {
        @Override
        public void visibilityChanged() {
            if (myProject.isOpen()) {
                PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(() -> myTodoTreeBuilder.setUpdatable(isShowing()));
            }
        }
    }

    private final class MyPreviewAction extends ToggleAction {
        MyPreviewAction() {
            super("Preview Source", null, PlatformIconGroup.actionsPreviewdetails());
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySettings.showPreview;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            mySettings.showPreview = state;
            myUsagePreviewPanelComponent.setVisible(state);
            if (state) {
                updatePreviewPanel();
            }
        }
    }
}
