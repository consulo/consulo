package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.HelpManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import consulo.ide.impl.idea.ui.TreeCopyProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeState;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.action.CollapseAllAction;
import consulo.ui.ex.awt.tree.action.ExpandAllAction;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.commited.*;
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

  public CommittedChangesTreeBrowser(final Project project, final List<CommittedChangeList> changeLists) {
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

    final TreeLinkMouseListener linkMouseListener = new TreeLinkMouseListener(new CommittedChangeListRenderer(project, myDecorators));
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
    final Splitter filterSplitter = new Splitter(false, 0.5f);

    filterSplitter.setSecondComponent(ScrollPaneFactory.createScrollPane(myChangesTree));
    myLeftPanel.add(filterSplitter, BorderLayout.CENTER);
    final Splitter mainSplitter = new Splitter(false, 0.7f);
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

  public void addFilter(final ChangeListFilteringStrategy strategy) {
    myFilteringStrategy.addStrategy(strategy.getKey(), strategy);
    strategy.addChangeListener(myFilterChangeListener);
  }

  private void updateGrouping() {
    if (myGroupingStrategy.changedSinceApply()) {
      myProject.getApplication().invokeLater(() -> updateModel(), IdeaModalityState.nonModal());
    }
  }

  private TreeModel buildTreeModel(final List<CommittedChangeList> filteredChangeLists) {
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

  public void setHelpId(final String helpId) {
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

  public void setItems(@Nonnull List<CommittedChangeList> items, final CommittedChangesBrowserUseCase useCase) {
    myDetailsView.setUseCase(useCase);
    myChangeLists = items;
    myFilteringStrategy.setFilterBase(items);
    myProject.getMessageBus().syncPublisher(CommittedChangesReloadListener.class).itemsReloaded();
    updateModel();
  }

  private void updateModel() {
    final List<CommittedChangeList> filteredChangeLists = myFilteringStrategy.filterChangeLists(myChangeLists);
    final TreePath[] paths = myChangesTree.getSelectionPaths();
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
    final TreePath[] selectionPaths = myChangesTree.getSelectionPaths();
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

  public static List<Change> collectChanges(final List<? extends CommittedChangeList> selectedChangeLists, final boolean withMovedTrees) {
    List<Change> result = new ArrayList<>();
    Collections.sort(selectedChangeLists, (o1, o2) -> o1.getCommitDate().compareTo(o2.getCommitDate()));
    for (CommittedChangeList cl : selectedChangeLists) {
      final Collection<Change> changes = withMovedTrees ? cl.getChangesWithMovedTrees() : cl.getChanges();
      for (Change c : changes) {
        addOrReplaceChange(result, c);
      }
    }
    return result;
  }

  /**
   * Zips changes by removing duplicates (changes in the same file) and compounding the diff.
   * <b>NB:</b> changes must be given in the time-ascending order, i.e the first change in the list should be the oldest one.
   */
  @Nonnull
  public static List<Change> zipChanges(@Nonnull List<Change> changes) {
    final List<Change> result = new ArrayList<>();
    for (Change change : changes) {
      addOrReplaceChange(result, change);
    }
    return result;
  }

  private static void addOrReplaceChange(final List<Change> changes, final Change c) {
    final ContentRevision beforeRev = c.getBeforeRevision();
    // todo!!! further improvements needed
    if (beforeRev != null) {
      final String beforeName = beforeRev.getFile().getName();
      final String beforeAbsolutePath = beforeRev.getFile().getIOFile().getAbsolutePath();
      for (Change oldChange : changes) {
        ContentRevision rev = oldChange.getAfterRevision();
        // first compare name, which is many times faster - to remove 99% not matching
        if (rev != null && (rev.getFile().getName().equals(beforeName))
          && rev.getFile().getIOFile().getAbsolutePath().equals(beforeAbsolutePath)) {
          changes.remove(oldChange);
          if (oldChange.getBeforeRevision() != null || c.getAfterRevision() != null) {
            changes.add(new Change(oldChange.getBeforeRevision(), c.getAfterRevision()));
          }
          return;
        }
      }
    }
    changes.add(c);
  }

  private List<CommittedChangeList> getSelectedChangeLists() {
    return TreeUtil.collectSelectedObjectsOfType(myChangesTree, CommittedChangeList.class);
  }

  public void setTableContextMenu(final ActionGroup group, final List<AnAction> auxiliaryActions) {
    DefaultActionGroup menuGroup = new DefaultActionGroup();
    menuGroup.add(group);
    for (AnAction action : auxiliaryActions) {
      menuGroup.add(action);
    }
    menuGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
    PopupHandler.installPopupHandler(myChangesTree, menuGroup, ActionPlaces.UNKNOWN, ActionManager.getInstance());
  }

  @Override
  public void removeFilteringStrategy(final CommittedChangesFilterKey key) {
    final ChangeListFilteringStrategy strategy = myFilteringStrategy.removeStrategy(key);
    if (strategy != null) {
      strategy.removeChangeListener(myFilterChangeListener);
    }
    myInnerSplitter.remove(key);
  }

  @Override
  public boolean setFilteringStrategy(final ChangeListFilteringStrategy filteringStrategy) {
    if (myInnerSplitter.canAdd()) {
      filteringStrategy.addChangeListener(myFilterChangeListener);

      final CommittedChangesFilterKey key = filteringStrategy.getKey();
      myFilteringStrategy.addStrategy(key, filteringStrategy);
      myFilteringStrategy.setFilterBase(myChangeLists);

      final JComponent filterUI = filteringStrategy.getFilterUI();
      if (filterUI != null) {
        myInnerSplitter.add(key, filterUI);
      }
      return true;
    }
    return false;
  }

  public ActionToolbar createGroupFilterToolbar(
    final Project project,
    final ActionGroup leadGroup,
    @Nullable final ActionGroup tailGroup,
    final List<AnAction> extra
  ) {
    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(leadGroup);
    toolbarGroup.addSeparator();
    toolbarGroup.add(new SelectFilteringAction(project, this));
    toolbarGroup.add(new SelectGroupingAction(project, this));
    final ExpandAllAction expandAllAction = new ExpandAllAction(myChangesTree);
    final CollapseAllAction collapseAllAction = new CollapseAllAction(myChangesTree);
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
      final Collection<Change> changes = collectChanges(getSelectedChangeLists(), false);
      sink.put(VcsDataKeys.CHANGES, changes.toArray(new Change[changes.size()]));
    }
    else if (VcsDataKeys.HAVE_SELECTED_CHANGES == dataId) {
      final int count = myChangesTree.getSelectionCount();
      sink.put(VcsDataKeys.HAVE_SELECTED_CHANGES, count > 0 ? Boolean.TRUE : Boolean.FALSE);
    }
    else if (VcsDataKeys.CHANGES_WITH_MOVED_CHILDREN == dataId) {
      final Collection<Change> changes = collectChanges(getSelectedChangeLists(), true);
      sink.put(VcsDataKeys.CHANGES_WITH_MOVED_CHILDREN, changes.toArray(new Change[changes.size()]));
    }
    else if (VcsDataKeys.CHANGE_LISTS == dataId) {
      final List<CommittedChangeList> lists = getSelectedChangeLists();
      if (!lists.isEmpty()) {
        sink.put(VcsDataKeys.CHANGE_LISTS, lists.toArray(new CommittedChangeList[lists.size()]));
      }
    }
    else if (Navigatable.KEY_OF_ARRAY == dataId) {
      final Collection<Change> changes = collectChanges(getSelectedChangeLists(), false);
      Navigatable[] result = ChangesUtil.getNavigatableArray(myProject, ChangesUtil.getFilesFromChanges(changes));
      sink.put(Navigatable.KEY_OF_ARRAY, result);
    }
    else if (HelpManager.HELP_ID == dataId) {
      sink.put(HelpManager.HELP_ID, myHelpId);
    }
    else if (VcsDataKeys.SELECTED_CHANGES_IN_DETAILS == dataId) {
      final List<Change> selectedChanges = myDetailsView.getSelectedChanges();
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
  public void install(final CommittedChangeListDecorator decorator) {
    myDecorators.add(decorator);
    repaintTree();
  }

  @Override
  public void remove(final CommittedChangeListDecorator decorator) {
    myDecorators.remove(decorator);
    repaintTree();
  }

  @Override
  public void reportLoadedLists(final CommittedChangeListsListener listener) {
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

  public void append(final List<CommittedChangeList> list) {
    final TreeState state = myChangeLists.isEmpty() && myState != null
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

    MoreLauncher(final Project project, final CommittedChangeList list) {
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
    public void calcData(final Key key, final DataSink sink) {
      if (CopyProvider.KEY == key) {
        sink.put(CopyProvider.KEY, myCopyProvider);
      }
      else if (PlatformDataKeys.TREE_EXPANDER == key) {
        sink.put(PlatformDataKeys.TREE_EXPANDER, myTreeExpander);
      }
      else {
        if (VcsDataKeys.SELECTED_CHANGES == key || VcsDataKeys.CHANGE_LEAD_SELECTION == key
          || CommittedChangesBrowserUseCase.DATA_KEY == key) {
          final Object data = myDetailsView.getData(key);
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
