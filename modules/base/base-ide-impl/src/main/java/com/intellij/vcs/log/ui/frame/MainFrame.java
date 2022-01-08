package com.intellij.vcs.log.ui.frame;

import com.google.common.primitives.Ints;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.TextRevisionNumber;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesTreeBrowser;
import com.intellij.openapi.vcs.changes.committed.RepositoryChangesBrowser;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.table.ComponentsListFocusTraversalPolicy;
import com.intellij.vcs.CommittedChangeListForRevision;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.MainVcsLogUiProperties;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.data.VcsLogProgress;
import com.intellij.vcs.log.data.VisiblePack;
import com.intellij.vcs.log.impl.VcsLogUtil;
import com.intellij.vcs.log.ui.VcsLogActionPlaces;
import com.intellij.vcs.log.ui.VcsLogInternalDataKeys;
import com.intellij.vcs.log.ui.VcsLogUiImpl;
import com.intellij.vcs.log.ui.actions.IntelliSortChooserPopupAction;
import com.intellij.vcs.log.ui.filter.VcsLogClassicFilterUi;
import com.intellij.vcs.log.util.BekUtil;
import com.intellij.vcs.log.util.VcsUserUtil;
import net.miginfocom.swing.MigLayout;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.intellij.util.ObjectUtils.assertNotNull;
import static com.intellij.util.containers.ContainerUtil.getFirstItem;

public class MainFrame extends JPanel implements DataProvider, Disposable {
  private static final String HELP_ID = "reference.changesToolWindow.log";

  @Nonnull
  private final VcsLogData myLogData;
  @Nonnull
  private final VcsLogUiImpl myUi;
  @Nonnull
  private final VcsLog myLog;
  @Nonnull
  private final VcsLogClassicFilterUi myFilterUi;

  @Nonnull
  private final JBLoadingPanel myChangesLoadingPane;
  @Nonnull
  private final VcsLogGraphTable myGraphTable;
  @Nonnull
  private final DetailsPanel myDetailsPanel;
  @Nonnull
  private final Splitter myDetailsSplitter;
  @Nonnull
  private final JComponent myToolbar;
  @Nonnull
  private final RepositoryChangesBrowser myChangesBrowser;
  @Nonnull
  private final Splitter myChangesBrowserSplitter;
  @Nonnull
  private final SearchTextField myTextFilter;
  @Nonnull
  private final MainVcsLogUiProperties myUiProperties;

  @Nonnull
  private Runnable myContainingBranchesListener;
  @Nonnull
  private Runnable myMiniDetailsLoadedListener;

  public MainFrame(@Nonnull VcsLogData logData,
                   @Nonnull VcsLogUiImpl ui,
                   @Nonnull Project project,
                   @Nonnull MainVcsLogUiProperties uiProperties,
                   @Nonnull VcsLog log,
                   @Nonnull VisiblePack initialDataPack) {
    // collect info
    myLogData = logData;
    myUi = ui;
    myLog = log;
    myUiProperties = uiProperties;

    myFilterUi = new VcsLogClassicFilterUi(myUi, logData, myUiProperties, initialDataPack);

    // initialize components
    myGraphTable = new VcsLogGraphTable(ui, logData, initialDataPack);
    myDetailsPanel = new DetailsPanel(logData, ui.getColorManager(), this);

    myChangesBrowser = new RepositoryChangesBrowser(project, null, Collections.emptyList(), null) {
      @Override
      protected void buildToolBar(DefaultActionGroup toolBarGroup) {
        super.buildToolBar(toolBarGroup);
        toolBarGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_SHOW_DETAILS_ACTION));
      }
    };
    myChangesBrowser.getViewerScrollPane().setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
    myChangesBrowser.getDiffAction().registerCustomShortcutSet(myChangesBrowser.getDiffAction().getShortcutSet(), getGraphTable());
    myChangesBrowser.getEditSourceAction().registerCustomShortcutSet(CommonShortcuts.getEditSource(), getGraphTable());
    myChangesBrowser.getViewer().setEmptyText("");
    myChangesLoadingPane = new JBLoadingPanel(new BorderLayout(), this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
    myChangesLoadingPane.add(myChangesBrowser);

    myDetailsSplitter = new OnePixelSplitter(true, "vcs.log.details.splitter.proportion", 0.7f);
    myDetailsSplitter.setFirstComponent(myChangesLoadingPane);
    setupDetailsSplitter(myUiProperties.get(MainVcsLogUiProperties.SHOW_DETAILS));

    myGraphTable.getSelectionModel().addListSelectionListener(new CommitSelectionListenerForDiff());
    myDetailsPanel.installCommitSelectionListener(myGraphTable);
    updateWhenDetailsAreLoaded();

    myTextFilter = myFilterUi.createTextFilter();
    myToolbar = createActionsToolbar();

    ProgressStripe progressStripe =
            new ProgressStripe(setupScrolledGraph(), this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS) {
              @Override
              public void updateUI() {
                super.updateUI();
                if (myDecorator != null && myLogData.getProgress().isRunning()) startLoadingImmediately();
              }
            };
    myLogData.getProgress().addProgressIndicatorListener(new VcsLogProgress.ProgressListener() {
      @Override
      public void progressStarted() {
        progressStripe.startLoading();
      }

      @Override
      public void progressStopped() {
        progressStripe.stopLoading();
      }
    }, this);


    JComponent toolbars = new JPanel(new BorderLayout());
    toolbars.add(myToolbar, BorderLayout.NORTH);
    JComponent toolbarsAndTable = new JPanel(new BorderLayout());
    toolbarsAndTable.add(toolbars, BorderLayout.NORTH);
    toolbarsAndTable.add(progressStripe, BorderLayout.CENTER);

    myChangesBrowserSplitter = new OnePixelSplitter(false, "vcs.log.changes.splitter.proportion", 0.7f);
    myChangesBrowserSplitter.setFirstComponent(toolbarsAndTable);
    myChangesBrowserSplitter.setSecondComponent(myDetailsSplitter);

    setLayout(new BorderLayout());
    add(myChangesBrowserSplitter);

    Disposer.register(ui, this);
    myGraphTable.resetDefaultFocusTraversalKeys();
    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new MyFocusPolicy());
  }

  /**
   * Informs components that the actual DataPack has been updated (e.g. due to a log refresh). <br/>
   * Components may want to update their fields and/or rebuild.
   *
   * @param dataPack         new data pack.
   * @param permGraphChanged true if permanent graph itself was changed.
   */
  public void updateDataPack(@Nonnull VisiblePack dataPack, boolean permGraphChanged) {
    myFilterUi.updateDataPack(dataPack);
    myGraphTable.updateDataPack(dataPack, permGraphChanged);
  }

  private void updateWhenDetailsAreLoaded() {
    myMiniDetailsLoadedListener = () -> {
      myGraphTable.initColumnSize();
      myGraphTable.repaint();
    };
    myContainingBranchesListener = () -> {
      myDetailsPanel.branchesChanged();
      myGraphTable.repaint(); // we may need to repaint highlighters
    };
    myLogData.getMiniDetailsGetter().addDetailsLoadedListener(myMiniDetailsLoadedListener);
    myLogData.getContainingBranchesGetter().addTaskCompletedListener(myContainingBranchesListener);
  }

  public void setupDetailsSplitter(boolean state) {
    myDetailsSplitter.setSecondComponent(state ? myDetailsPanel : null);
  }

  @Nonnull
  private JScrollPane setupScrolledGraph() {
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myGraphTable, SideBorder.TOP);
    myGraphTable.viewportSet(scrollPane.getViewport());
    return scrollPane;
  }

  @Nonnull
  public VcsLogGraphTable getGraphTable() {
    return myGraphTable;
  }

  @Nonnull
  public VcsLogFilterUi getFilterUi() {
    return myFilterUi;
  }

  private JComponent createActionsToolbar() {
    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.TOOLBAR_ACTION_GROUP));

    DefaultActionGroup mainGroup = new DefaultActionGroup();
    mainGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_TEXT_FILTER_SETTINGS_ACTION));
    mainGroup.add(new AnSeparator());
    mainGroup.add(myFilterUi.createActionGroup());
    mainGroup.addSeparator();
    if (BekUtil.isBekEnabled()) {
      if (BekUtil.isLinearBekEnabled()) {
        mainGroup.add(new IntelliSortChooserPopupAction());
        // can not register both of the actions in xml file, choosing to register an action for the "outer world"
        // I can of course if linear bek is enabled replace the action on start but why bother
      }
      else {
        mainGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_INTELLI_SORT_ACTION));
      }
    }
    mainGroup.add(toolbarGroup);
    ActionToolbar toolbar = createActionsToolbar(mainGroup);

    Wrapper textFilter = new Wrapper(myTextFilter);
    textFilter.setVerticalSizeReferent(toolbar.getComponent());
    textFilter.setBorder(JBUI.Borders.emptyLeft(5));

    ActionToolbar settings =
            createActionsToolbar(new DefaultActionGroup(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_QUICK_SETTINGS_ACTION)));
    settings.setReservePlaceAutoPopupIcon(false);
    settings.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

    JPanel panel = new JPanel(new MigLayout("ins 0, fill", "[left]0[left, fill]push[right]", "center"));
    panel.add(textFilter);
    panel.add(toolbar.getComponent());
    panel.add(settings.getComponent());
    return panel;
  }

  @Nonnull
  private ActionToolbar createActionsToolbar(@Nonnull DefaultActionGroup mainGroup) {
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, mainGroup, true);
    toolbar.setTargetComponent(this);
    return toolbar;
  }

  @Nonnull
  public JComponent getMainComponent() {
    return this;
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (VcsLogDataKeys.VCS_LOG == dataId) {
      return myLog;
    }
    else if (VcsLogDataKeys.VCS_LOG_UI == dataId) {
      return myUi;
    }
    else if (VcsLogDataKeys.VCS_LOG_DATA_PROVIDER == dataId) {
      return myLogData;
    }
    else if (VcsDataKeys.CHANGES == dataId || VcsDataKeys.SELECTED_CHANGES == dataId) {
      return ArrayUtil.toObjectArray(myChangesBrowser.getCurrentDisplayedChanges(), Change.class);
    }
    else if (VcsDataKeys.CHANGE_LISTS == dataId) {
      List<VcsFullCommitDetails> details = myLog.getSelectedDetails();
      if (details.size() > VcsLogUtil.MAX_SELECTED_COMMITS) return null;
      return ContainerUtil.map2Array(details, CommittedChangeListForRevision.class,
                                     detail -> new CommittedChangeListForRevision(detail.getSubject(), detail.getFullMessage(),
                                                                                  VcsUserUtil.getShortPresentation(detail.getCommitter()),
                                                                                  new Date(detail.getCommitTime()),
                                                                                  detail.getChanges(),
                                                                                  convertToRevisionNumber(detail.getId())));
    }
    else if (VcsDataKeys.VCS_REVISION_NUMBERS == dataId) {
      List<CommitId> hashes = myLog.getSelectedCommits();
      if (hashes.size() > VcsLogUtil.MAX_SELECTED_COMMITS) return null;
      return ArrayUtil
              .toObjectArray(ContainerUtil.map(hashes, commitId -> convertToRevisionNumber(commitId.getHash())), VcsRevisionNumber.class);
    }
    else if (VcsDataKeys.VCS == dataId) {
      int[] selectedRows = myGraphTable.getSelectedRows();
      if (selectedRows.length == 0 || selectedRows.length > VcsLogUtil.MAX_SELECTED_COMMITS) return null;
      Set<VirtualFile> roots = ContainerUtil.map2Set(Ints.asList(selectedRows), row -> myGraphTable.getModel().getRoot(row));
      if (roots.size() == 1) {
        return myLogData.getLogProvider(assertNotNull(getFirstItem(roots))).getSupportedVcs();
      }
    }
    else if (VcsLogDataKeys.VCS_LOG_BRANCHES == dataId) {
      int[] selectedRows = myGraphTable.getSelectedRows();
      if (selectedRows.length != 1) return null;
      return myGraphTable.getModel().getBranchesAtRow(selectedRows[0]);
    }
    else if (PlatformDataKeys.HELP_ID == dataId) {
      return HELP_ID;
    }
    else if (VcsLogInternalDataKeys.LOG_UI_PROPERTIES == dataId) {
      return myUiProperties;
    }
    return null;
  }

  @Nonnull
  public JComponent getToolbar() {
    return myToolbar;
  }

  @Nonnull
  public SearchTextField getTextFilter() {
    return myTextFilter;
  }

  public boolean areGraphActionsEnabled() {
    return myGraphTable.getRowCount() > 0;
  }

  @Nonnull
  private static TextRevisionNumber convertToRevisionNumber(@Nonnull Hash hash) {
    return new TextRevisionNumber(hash.asString(), hash.toShortString());
  }

  public void showDetails(boolean state) {
    myDetailsSplitter.setSecondComponent(state ? myDetailsPanel : null);
  }

  @Override
  public void dispose() {
    myLogData.getMiniDetailsGetter().removeDetailsLoadedListener(myMiniDetailsLoadedListener);
    myLogData.getContainingBranchesGetter().removeTaskCompletedListener(myContainingBranchesListener);

    myDetailsSplitter.dispose();
    myChangesBrowserSplitter.dispose();
  }

  private class CommitSelectionListenerForDiff extends CommitSelectionListener {
    protected CommitSelectionListenerForDiff() {
      super(myLogData, MainFrame.this.myGraphTable, myChangesLoadingPane);
    }

    @Override
    protected void onDetailsLoaded(@Nonnull List<VcsFullCommitDetails> detailsList) {
      List<Change> changes = ContainerUtil.newArrayList();
      List<VcsFullCommitDetails> detailsListReversed = ContainerUtil.reverse(detailsList);
      for (VcsFullCommitDetails details : detailsListReversed) {
        changes.addAll(details.getChanges());
      }
      changes = CommittedChangesTreeBrowser.zipChanges(changes);
      myChangesBrowser.setChangesToDisplay(changes);
    }

    @Override
    protected void onSelection(@Nonnull int[] selection) {
      // just reset and wait for details to be loaded
      myChangesBrowser.setChangesToDisplay(Collections.emptyList());
      myChangesBrowser.getViewer().setEmptyText("");
    }

    @Override
    protected void onEmptySelection() {
      myChangesBrowser.getViewer().setEmptyText("No commits selected");
      myChangesBrowser.setChangesToDisplay(Collections.emptyList());
    }
  }

  private class MyFocusPolicy extends ComponentsListFocusTraversalPolicy {
    @Nonnull
    @Override
    protected List<Component> getOrderedComponents() {
      return Arrays.asList(myGraphTable, myChangesBrowser.getPreferredFocusedComponent(), myTextFilter.getTextEditor());
    }
  }
}
