package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.HelpManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ui.TreeCopyProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.tree.action.CollapseAllAction;
import consulo.ui.ex.awt.tree.action.ExpandAllAction;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.commited.*;
import consulo.versionControlSystem.internal.ChangesBrowserUtil;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author yole
 */
public class CommittedChangesTreeBrowser extends JPanel implements TypeSafeDataProvider, Disposable, DecoratorManager {
  private static final Border RIGHT_BORDER = IdeBorderFactory.createBorder(SideBorder.TOP | SideBorder.LEFT);

  private final Project myProject;
  private final Tree myChangesTree;
  private final RepositoryChangesBrowser myDetailsView;
  private List<CommittedChangeList> myChangeLists;
  private List<CommittedChangeList> mySelectedChangeLists;
  private ChangeListGroupingStrategy myGroupingStrategy = new DateChangeListGroupingStrategy();
  private final CompositeChangeListFilteringStrategy myFilteringStrategy = new CompositeChangeListFilteringStrategy();
  private final JPanel myLeftPanel;
  private final FilterChangeListener myFilterChangeListener = new FilterChangeListener();
  private final SplitterProportionsData mySplitterProportionsData = new SplitterProportionsDataImpl();
  private final CopyProvider myCopyProvider;
  private final TreeExpander myTreeExpander;
  private String myHelpId;

  private final List<CommittedChangeListDecorator> myDecorators;

  @NonNls public static final String ourHelpId = "reference.changesToolWindow.incoming";

  private WiseSplitter myInnerSplitter;
  private final MessageBusConnection myConnection;
  private TreeState myState;

  public CommittedChangesTreeBrowser(Project project, List<CommittedChangeList> changeLists) {
    super(new BorderLayout());

    myProject = project;
    myDecorators = new LinkedList<>();
    myChangeLists = changeLists;
    myChangesTree = new ChangesBrowserTree();
    myChangesTree.setRootVisible(false);
    myChangesTree.setShowsRootHandles(true);
    myChangesTree.setCellRenderer(new CommittedChangeListRenderer(project, myDecorators));
    TreeUtil.expandAll(myChangesTree);
    myChangesTree.getExpandableItemsHandler().setEnabled(false);

    myDetailsView = new RepositoryChangesBrowser(project, Collections.<CommittedChangeList>emptyList());
    myDetailsView.getViewer().setScrollPaneBorder(RIGHT_BORDER);

    myChangesTree.getSelectionModel().addTreeSelectionListener(e -> updateBySelectionChange());

    TreeLinkMouseListener linkMouseListener = new TreeLinkMouseListener(new CommittedChangeListRenderer(project, myDecorators));
    linkMouseListener.installOn(myChangesTree);

    myLeftPanel = new JPanel(new BorderLayout());

    initSplitters();

    updateBySelectionChange();

    ActionManager.getInstance().getAction("CommittedChanges.Details")
            .registerCustomShortcutSet(new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_QUICK_JAVADOC)),
                                       this);

    myCopyProvider = new TreeCopyProvider(myChangesTree);
    myTreeExpander = new DefaultTreeExpander(myChangesTree);
    myDetailsView.addToolbarAction(ActionManager.getInstance().getAction("Vcs.ShowTabbedFileHistory"));

    myHelpId = ourHelpId;

    myDetailsView.getDiffAction().registerCustomShortcutSet(CommonShortcuts.getDiff(), myChangesTree);

    myConnection = myProject.getMessageBus().connect();
    myConnection.subscribe(CommittedChangesReloadListener.class, new CommittedChangesReloadListener() {
      @Override
      public void itemsReloaded() {
      }

      @Override
      public void emptyRefresh() {
        updateGrouping();
      }
    });
  }

  private void initSplitters() {
    Splitter filterSplitter = new Splitter(false, 0.5f);

    filterSplitter.setSecondComponent(ScrollPaneFactory.createScrollPane(myChangesTree));
    myLeftPanel.add(filterSplitter, BorderLayout.CENTER);
    Splitter mainSplitter = new Splitter(false, 0.7f);
    mainSplitter.setFirstComponent(myLeftPanel);
    mainSplitter.setSecondComponent(myDetailsView);

    add(mainSplitter, BorderLayout.CENTER);

    myInnerSplitter = new WiseSplitter(() -> {
      filterSplitter.doLayout();
      updateModel();
    }, filterSplitter);
    Disposer.register(this, myInnerSplitter);

    mySplitterProportionsData.externalizeFromDimensionService("CommittedChanges.SplitterProportions");
    mySplitterProportionsData.restoreSplitterProportions(this);
  }

  public void addFilter(ChangeListFilteringStrategy strategy) {
    myFilteringStrategy.addStrategy(strategy.getKey(), strategy);
    strategy.addChangeListener(myFilterChangeListener);
  }

  private void updateGrouping() {
    if (myGroupingStrategy.changedSinceApply()) {
      myProject.getApplication().invokeLater(() -> updateModel(), IdeaModalityState.nonModal());
    }
  }

  private TreeModel buildTreeModel(List<CommittedChangeList> filteredChangeLists) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    DefaultTreeModel model = new DefaultTreeModel(root);
    Collections.sort(filteredChangeLists, myGroupingStrategy.getComparator());
    myGroupingStrategy.beforeStart();
    DefaultMutableTreeNode lastGroupNode = null;
    String lastGroupName = null;
    for (CommittedChangeList list : filteredChangeLists) {
      String groupName = myGroupingStrategy.getGroupName(list);
      if (!Comparing.equal(groupName, lastGroupName)) {
        lastGroupName = groupName;
        lastGroupNode = new DefaultMutableTreeNode(lastGroupName);
        root.add(lastGroupNode);
      }
      assert lastGroupNode != null;
      lastGroupNode.add(new DefaultMutableTreeNode(list));
    }
    return model;
  }

  public void setHelpId(String helpId) {
    myHelpId = helpId;
  }

  public StatusText getEmptyText() {
    return myChangesTree.getEmptyText();
  }

  public void setToolBar(JComponent toolBar) {
    myLeftPanel.add(toolBar, BorderLayout.NORTH);
    Dimension prefSize = myDetailsView.getHeaderPanel().getPreferredSize();
    if (prefSize.height < toolBar.getPreferredSize().height) {
      prefSize.height = toolBar.getPreferredSize().height;
      myDetailsView.getHeaderPanel().setPreferredSize(prefSize);
    }
  }

  public void dispose() {
    myConnection.disconnect();
    mySplitterProportionsData.saveSplitterProportions(this);
    mySplitterProportionsData.externalizeToDimensionService("CommittedChanges.SplitterProportions");
    myDetailsView.dispose();
  }

  public void setItems(@Nonnull List<CommittedChangeList> items, CommittedChangesBrowserUseCase useCase) {
    myDetailsView.setUseCase(useCase);
    myChangeLists = items;
    myFilteringStrategy.setFilterBase(items);
    myProject.getMessageBus().syncPublisher(CommittedChangesReloadListener.class).itemsReloaded();
    updateModel();
  }

  private void updateModel() {
    List<CommittedChangeList> filteredChangeLists = myFilteringStrategy.filterChangeLists(myChangeLists);
    TreePath[] paths = myChangesTree.getSelectionPaths();
    myChangesTree.setModel(buildTreeModel(filteredChangeLists));
    TreeUtil.expandAll(myChangesTree);
    myChangesTree.setSelectionPaths(paths);
  }

  public void setGroupingStrategy(ChangeListGroupingStrategy strategy) {
    myGroupingStrategy = strategy;
    updateModel();
  }

  private void updateBySelectionChange() {
    List<CommittedChangeList> selection = new ArrayList<>();
    TreePath[] selectionPaths = myChangesTree.getSelectionPaths();
    if (selectionPaths != null) {
      for (TreePath path : selectionPaths) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        if (node.getUserObject() instanceof CommittedChangeList) {
          selection.add((CommittedChangeList)node.getUserObject());
        }
      }
    }

    if (!selection.equals(mySelectedChangeLists)) {
      mySelectedChangeLists = selection;
      myDetailsView.setChangesToDisplay(collectChanges(mySelectedChangeLists, false));
    }
  }

  public static List<Change> collectChanges(List<? extends CommittedChangeList> selectedChangeLists, boolean withMovedTrees) {
    List<Change> result = new ArrayList<>();
    Collections.sort(selectedChangeLists, (o1, o2) -> o1.getCommitDate().compareTo(o2.getCommitDate()));
    for (CommittedChangeList cl : selectedChangeLists) {
      Collection<Change> changes = withMovedTrees ? cl.getChangesWithMovedTrees() : cl.getChanges();
      for (Change c : changes) {
          ChangesBrowserUtil.addOrReplaceChange(result, c);
      }
    }
    return result;
  }

  private List<CommittedChangeList> getSelectedChangeLists() {
    return TreeUtil.collectSelectedObjectsOfType(myChangesTree, CommittedChangeList.class);
  }

  public void setTableContextMenu(ActionGroup group, List<AnAction> auxiliaryActions) {
    DefaultActionGroup menuGroup = new DefaultActionGroup();
    menuGroup.add(group);
    for (AnAction action : auxiliaryActions) {
      menuGroup.add(action);
    }
    menuGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
    PopupHandler.installPopupHandler(myChangesTree, menuGroup, ActionPlaces.UNKNOWN, ActionManager.getInstance());
  }

  @Override
  public void removeFilteringStrategy(CommittedChangesFilterKey key) {
    ChangeListFilteringStrategy strategy = myFilteringStrategy.removeStrategy(key);
    if (strategy != null) {
      strategy.removeChangeListener(myFilterChangeListener);
    }
    myInnerSplitter.remove(key);
  }

  @Override
  public boolean setFilteringStrategy(ChangeListFilteringStrategy filteringStrategy) {
    if (myInnerSplitter.canAdd()) {
      filteringStrategy.addChangeListener(myFilterChangeListener);

      CommittedChangesFilterKey key = filteringStrategy.getKey();
      myFilteringStrategy.addStrategy(key, filteringStrategy);
      myFilteringStrategy.setFilterBase(myChangeLists);

      JComponent filterUI = filteringStrategy.getFilterUI();
      if (filterUI != null) {
        myInnerSplitter.add(key, filterUI);
      }
      return true;
    }
    return false;
  }

  public ActionToolbar createGroupFilterToolbar(
    Project project,
    ActionGroup leadGroup,
    @Nullable ActionGroup tailGroup,
    List<AnAction> extra
  ) {
    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(leadGroup);
    toolbarGroup.addSeparator();
    toolbarGroup.add(new SelectFilteringAction(project, this));
    toolbarGroup.add(new SelectGroupingAction(project, this));
    ExpandAllAction expandAllAction = new ExpandAllAction(myChangesTree);
    CollapseAllAction collapseAllAction = new CollapseAllAction(myChangesTree);
    expandAllAction.registerCustomShortcutSet(
      new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_EXPAND_ALL)),
      myChangesTree
    );
    collapseAllAction.registerCustomShortcutSet(
      new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_COLLAPSE_ALL)),
      myChangesTree
    );
    toolbarGroup.add(expandAllAction);
    toolbarGroup.add(collapseAllAction);
    toolbarGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
    toolbarGroup.add(new ContextHelpAction(myHelpId));
    if (tailGroup != null) {
      toolbarGroup.add(tailGroup);
    }
    for (AnAction anAction : extra) {
      toolbarGroup.add(anAction);
    }
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarGroup, true);
  }

  @Override
  public void calcData(Key<?> dataId, DataSink sink) {
    if (VcsDataKeys.CHANGES == dataId) {
      Collection<Change> changes = collectChanges(getSelectedChangeLists(), false);
      sink.put(VcsDataKeys.CHANGES, changes.toArray(new Change[changes.size()]));
    }
    else if (VcsDataKeys.HAVE_SELECTED_CHANGES == dataId) {
      int count = myChangesTree.getSelectionCount();
      sink.put(VcsDataKeys.HAVE_SELECTED_CHANGES, count > 0 ? Boolean.TRUE : Boolean.FALSE);
    }
    else if (VcsDataKeys.CHANGES_WITH_MOVED_CHILDREN == dataId) {
      Collection<Change> changes = collectChanges(getSelectedChangeLists(), true);
      sink.put(VcsDataKeys.CHANGES_WITH_MOVED_CHILDREN, changes.toArray(new Change[changes.size()]));
    }
    else if (VcsDataKeys.CHANGE_LISTS == dataId) {
      List<CommittedChangeList> lists = getSelectedChangeLists();
      if (!lists.isEmpty()) {
        sink.put(VcsDataKeys.CHANGE_LISTS, lists.toArray(new CommittedChangeList[lists.size()]));
      }
    }
    else if (Navigatable.KEY_OF_ARRAY == dataId) {
      Collection<Change> changes = collectChanges(getSelectedChangeLists(), false);
      Navigatable[] result = ChangesUtil.getNavigatableArray(myProject, ChangesUtil.getFilesFromChanges(changes));
      sink.put(Navigatable.KEY_OF_ARRAY, result);
    }
    else if (HelpManager.HELP_ID == dataId) {
      sink.put(HelpManager.HELP_ID, myHelpId);
    }
    else if (VcsDataKeys.SELECTED_CHANGES_IN_DETAILS == dataId) {
      List<Change> selectedChanges = myDetailsView.getSelectedChanges();
      sink.put(VcsDataKeys.SELECTED_CHANGES_IN_DETAILS, selectedChanges.toArray(new Change[selectedChanges.size()]));
    }
  }

  public TreeExpander getTreeExpander() {
    return myTreeExpander;
  }

  @Override
  public void repaintTree() {
    myChangesTree.revalidate();
    myChangesTree.repaint();
  }

  @Override
  public void install(CommittedChangeListDecorator decorator) {
    myDecorators.add(decorator);
    repaintTree();
  }

  @Override
  public void remove(CommittedChangeListDecorator decorator) {
    myDecorators.remove(decorator);
    repaintTree();
  }

  @Override
  public void reportLoadedLists(CommittedChangeListsListener listener) {
    myProject.getApplication().executeOnPooledThread((Runnable) () -> {
      listener.onBeforeStartReport();
      for (CommittedChangeList list : myChangeLists) {
        listener.report(list);
      }
      listener.onAfterEndReport();
    });
  }

  // for appendable view
  public void reset() {
    myChangeLists.clear();
    myFilteringStrategy.resetFilterBase();

    myState = TreeState.createOn(myChangesTree, (DefaultMutableTreeNode)myChangesTree.getModel().getRoot());
    updateModel();
  }

  public void append(List<CommittedChangeList> list) {
    TreeState state = myChangeLists.isEmpty() && myState != null
                            ? myState
                            : TreeState.createOn(myChangesTree, (DefaultMutableTreeNode)myChangesTree.getModel().getRoot());
    state.setScrollToSelection(false);
    myChangeLists.addAll(list);

    myFilteringStrategy.appendFilterBase(list);

    myChangesTree.setModel(buildTreeModel(myFilteringStrategy.filterChangeLists(myChangeLists)));
    state.applyTo(myChangesTree, myChangesTree.getModel().getRoot());
    TreeUtil.expandAll(myChangesTree);
    myProject.getMessageBus().syncPublisher(CommittedChangesReloadListener.class).itemsReloaded();
  }

  public static class MoreLauncher implements Runnable {
    private final Project myProject;
    private final CommittedChangeList myList;

    MoreLauncher(Project project, CommittedChangeList list) {
      myProject = project;
      myList = list;
    }

    @Override
    public void run() {
      ChangeListDetailsAction.showDetailsPopup(myProject, myList);
    }
  }

  private class FilterChangeListener implements ChangeListener {
    @Override
    public void stateChanged(ChangeEvent e) {
      if (myProject.getApplication().isDispatchThread()) {
        updateModel();
      }
      else {
        myProject.getApplication().invokeLater(() -> updateModel());
      }
    }
  }

  private class ChangesBrowserTree extends Tree implements TypeSafeDataProvider {
    public ChangesBrowserTree() {
      super(buildTreeModel(myFilteringStrategy.filterChangeLists(myChangeLists)));
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public void calcData(Key key, DataSink sink) {
      if (CopyProvider.KEY == key) {
        sink.put(CopyProvider.KEY, myCopyProvider);
      }
      else if (PlatformDataKeys.TREE_EXPANDER == key) {
        sink.put(PlatformDataKeys.TREE_EXPANDER, myTreeExpander);
      }
      else {
        if (VcsDataKeys.SELECTED_CHANGES == key || VcsDataKeys.CHANGE_LEAD_SELECTION == key
          || CommittedChangesBrowserUseCase.DATA_KEY == key) {
          Object data = myDetailsView.getData(key);
          if (data != null) {
            sink.put(key, data);
          }
        }
      }
    }
  }

  public void setLoading(final boolean value) {
    new AbstractCalledLater(myProject, IdeaModalityState.nonModal()) {
      @Override
      public void run() {
        myChangesTree.setPaintBusy(value);
      }
    }.callMe();
  }
}
