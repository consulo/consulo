// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.todo;

import consulo.application.AllIcons;
import consulo.application.HelpManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.util.TodoPanelSettings;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.find.FindModel;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.ide.actions.NextOccurenceToolbarAction;
import consulo.ide.impl.idea.ide.actions.PreviousOccurenceToolbarAction;
import consulo.ide.impl.idea.ide.todo.nodes.TodoFileNode;
import consulo.ide.impl.idea.ide.todo.nodes.TodoItemNode;
import consulo.ide.impl.idea.ide.todo.nodes.TodoTreeHelper;
import consulo.ide.impl.idea.openapi.wm.impl.VisibilityWatcher;
import consulo.ide.impl.idea.usages.impl.UsagePreviewPanel;
import consulo.ide.localize.IdeLocalize;
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
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.usage.UsageInfo;
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

abstract class TodoPanel extends SimpleToolWindowPanel implements OccurenceNavigator, DataProvider, Disposable {
  protected static final Logger LOG = Logger.getInstance(TodoPanel.class);

  protected Project myProject;
  private final TodoPanelSettings mySettings;
  private final boolean myCurrentFileMode;
  private final Content myContent;

  private final Tree myTree;
  private final MyTreeExpander myTreeExpander;
  private final MyOccurenceNavigator myOccurenceNavigator;
  protected final TodoTreeBuilder myTodoTreeBuilder;
  private MyVisibilityWatcher myVisibilityWatcher;
  private UsagePreviewPanel myUsagePreviewPanel;
  private MyAutoScrollToSourceHandler myAutoScrollToSourceHandler;

  public static final Key<TodoPanel> TODO_PANEL_DATA_KEY = Key.create("TodoPanel");

  /**
   * @param currentFileMode if {@code true} then view doesn't have "KeymapGroupImpl By Packages" and "Flatten Packages"
   *                        actions.
   */
  TodoPanel(Project project, TodoPanelSettings settings, boolean currentFileMode, Content content) {
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

  public static class GroupByActionGroup extends DefaultActionGroup {
    {
      getTemplatePresentation().setIcon(PlatformIconGroup.actionsGroupby());
      getTemplatePresentation().setTextValue(IdeLocalize.groupGroupBy());
      setPopup(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      JBPopupFactory.getInstance().createActionGroupPopup(
        null,
        this,
        e.getDataContext(),
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        true
      ).showUnderneathOf(e.getInputEvent().getComponent());
    }
  }

  private class MyExpandListener extends TreeModelAdapter {

    private final TodoTreeBuilder myBuilder;

    MyExpandListener(TodoTreeBuilder builder) {
      myBuilder = builder;
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      TreePath parentPath = e.getTreePath();
      if (parentPath == null || parentPath.getPathCount() > 2) return;
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
          final Object userObject = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
          if (!((userObject instanceof NodeDescriptor ? (NodeDescriptor)userObject : null) instanceof TodoItemNode)) {
            return;
          }
          OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(TodoPanel.this), false);
        }
      }
    });


    myUsagePreviewPanel = new UsagePreviewPanel(myProject, FindInProjectUtil.setupViewPresentation(false, new FindModel()));
    Disposer.register(this, myUsagePreviewPanel);
    myUsagePreviewPanel.setVisible(mySettings.showPreview);

    setContent(createCenterComponent());

    myTree.getSelectionModel().addTreeSelectionListener(e -> myProject.getApplication().invokeLater(
      () -> {
        if (myUsagePreviewPanel.isVisible()) {
          updatePreviewPanel();
        }
      },
      IdeaModalityState.nonModal(),
      myProject.getDisposed()
    ));

    myAutoScrollToSourceHandler = new MyAutoScrollToSourceHandler();
    myAutoScrollToSourceHandler.install(myTree);

    // Create tool bars and register custom shortcuts

    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(new PreviousOccurenceToolbarAction(myOccurenceNavigator));
    toolbarGroup.add(new NextOccurenceToolbarAction(myOccurenceNavigator));
    toolbarGroup.add(new SetTodoFilterAction(myProject, mySettings, todoFilter -> setTodoFilter(todoFilter)));
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
    return (DefaultActionGroup)actionManager.getAction("TodoViewGroupByGroup");
  }

  protected AnAction createAutoScrollToSourceAction() {
    return myAutoScrollToSourceHandler.createToggleAction();
  }

  protected JComponent createCenterComponent() {
    Splitter splitter = new OnePixelSplitter(false);
    splitter.setSecondComponent(myUsagePreviewPanel);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myTree));
    return splitter;
  }

  private void updatePreviewPanel() {
    if (myProject == null || myProject.isDisposed()) return;
    List<UsageInfo> infos = new ArrayList<>();
    final TreePath path = myTree.getSelectionPath();
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject instanceof NodeDescriptor) {
        Object element = ((NodeDescriptor)userObject).getElement();
        TodoItemNode pointer = myTodoTreeBuilder.getFirstPointerForElement(element);
        if (pointer != null) {
          final SmartTodoItemPointer value = pointer.getValue();
          final Document document = value.getDocument();
          final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
          final RangeMarker rangeMarker = value.getRangeMarker();
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
    myProject = null;
  }

  void rebuildCache() {
    myTodoTreeBuilder.rebuildCache();
  }

  void rebuildCache(@Nonnull Set<VirtualFile> files) {
    myTodoTreeBuilder.rebuildCache(files);
  }

  /**
   * Immediately updates tree.
   */
  void updateTree() {
    myTodoTreeBuilder.updateTree();
  }

  /**
   * Updates current filter. If previously set filter was removed then empty filter is set.
   *
   * @see TodoTreeBuilder#setTodoFilter
   */
  void updateTodoFilter() {
    TodoFilter filter = TodoConfiguration.getInstance().getTodoFilter(mySettings.todoFilterName);
    setTodoFilter(filter);
  }

  /**
   * Sets specified {@code TodoFilter}. The method also updates window's title.
   *
   * @see TodoTreeBuilder#setTodoFilter
   */
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
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
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
    if (myTree == null) return null;
    TreePath path = myTree.getSelectionPath();
    if (path == null) {
      return null;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    Object userObject = node.getUserObject();
    final PsiElement selectedElement = TodoTreeHelper.getSelectedElement(userObject);
    if (selectedElement != null) return selectedElement;
    return getSelectedFile();
  }

  @Override
  public Object getData(@Nonnull Key dataId) {
    if (Navigatable.KEY == dataId) {
      TreePath path = myTree.getSelectionPath();
      if (path == null) {
        return null;
      }
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
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
      final PsiFile file = getSelectedFile();
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

  @Override
  @Nullable
  public OccurenceInfo goPreviousOccurence() {
    return myOccurenceNavigator.goPreviousOccurence();
  }

  @Nonnull
  @Override
  public String getNextOccurenceActionName() {
    return myOccurenceNavigator.getNextOccurenceActionName();
  }

  @Override
  @Nullable
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

  protected void rebuildWithAlarm(final Alarm alarm) {
    alarm.cancelAllRequests();
    alarm.addRequest(() -> {
      final Set<VirtualFile> files = new HashSet<>();
      DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
        if (myTodoTreeBuilder.isDisposed()) return;
        myTodoTreeBuilder.collectFiles(virtualFile -> {
          files.add(virtualFile);
          return true;
        });
        final Runnable runnable = () -> {
          if (myTodoTreeBuilder.isDisposed()) return;
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
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (userObject == null) {
        return false;
      }
      if (userObject instanceof NodeDescriptor && ((NodeDescriptor)userObject).getElement() instanceof TodoItemNode) {
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
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      return userObject instanceof NodeDescriptor && !isFirst(node);
    }

    private boolean isFirst(final TreeNode node) {
      final TreeNode parent = node.getParent();
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
      return IdeLocalize.actionNextTodo().get();
    }

    @Nonnull
    @Override
    public String getPreviousOccurenceActionName() {
      return IdeLocalize.actionPreviousTodo().get();
    }

    @Nullable
    private OccurenceInfo goToPointer(TodoItemNode pointer) {
      if (pointer == null) return null;
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
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
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
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object userObject = node.getUserObject();
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      Object element = ((NodeDescriptor)userObject).getElement();
      TodoItemNode pointer;
      if (element instanceof TodoItemNode) {
        pointer = myTodoTreeBuilder.getPreviousPointer((TodoItemNode)element);
      }
      else {
        Object sibling = myTodoTreeBuilder.getPreviousSibling(element);
        if (sibling == null) {
          return null;
        }
        pointer = myTodoTreeBuilder.getLastPointerForElement(sibling);
      }
      return pointer;
    }
  }

  public static final class MyShowPackagesAction extends ToggleAction {
    public MyShowPackagesAction() {
      super(IdeBundle.message("action.group.by.packages"), null, AllIcons.Actions.GroupByPackage);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getData(TODO_PANEL_DATA_KEY) != null);
      super.update(e);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.arePackagesShown;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      if (todoPanel != null) {
        todoPanel.mySettings.arePackagesShown = state;
        todoPanel.myTodoTreeBuilder.setShowPackages(state);
      }
    }
  }

  public static final class MyShowModulesAction extends ToggleAction {
    public MyShowModulesAction() {
      super(IdeLocalize.actionGroupByModules(), LocalizeValue.empty(), AllIcons.Actions.GroupByModule);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getData(TODO_PANEL_DATA_KEY) != null);
      super.update(e);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.areModulesShown;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);

      if (todoPanel != null) {
        todoPanel.mySettings.areModulesShown = state;
        todoPanel.myTodoTreeBuilder.setShowModules(state);
      }
    }
  }

  public static final class MyFlattenPackagesAction extends ToggleAction {
    public MyFlattenPackagesAction() {
      super(IdeLocalize.actionFlattenView(), LocalizeValue.empty(), AllIcons.ObjectBrowser.FlattenPackages);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setText("   " + getTemplateText());
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      e.getPresentation().setEnabled(todoPanel != null && todoPanel.mySettings.arePackagesShown);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      return todoPanel != null && todoPanel.mySettings.areFlattenPackages;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
      if (todoPanel != null) {
        todoPanel.mySettings.areFlattenPackages = state;
        todoPanel.myTodoTreeBuilder.setFlattenPackages(state);
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
      super("Preview Source", null, AllIcons.Actions.PreviewDetails);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return mySettings.showPreview;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      mySettings.showPreview = state;
      myUsagePreviewPanel.setVisible(state);
      if (state) {
        updatePreviewPanel();
      }
    }
  }
}
