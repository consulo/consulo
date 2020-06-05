package com.intellij.vcs.log.ui;

import com.google.common.util.concurrent.SettableFuture;
import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import consulo.disposer.Disposer;
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.PairFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.*;
import com.intellij.vcs.log.data.MainVcsLogUiProperties.VcsLogHighlighterProperty;
import com.intellij.vcs.log.graph.PermanentGraph;
import com.intellij.vcs.log.graph.actions.GraphAction;
import com.intellij.vcs.log.graph.actions.GraphAnswer;
import com.intellij.vcs.log.impl.VcsLogImpl;
import com.intellij.vcs.log.ui.frame.MainFrame;
import com.intellij.vcs.log.ui.frame.VcsLogGraphTable;
import com.intellij.vcs.log.ui.tables.GraphTableModel;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

public class VcsLogUiImpl implements VcsLogUi, Disposable {
  private static final Logger LOG = Logger.getInstance(VcsLogUiImpl.class);
  public static final ExtensionPointName<VcsLogHighlighterFactory> LOG_HIGHLIGHTER_FACTORY_EP =
          ExtensionPointName.create("com.intellij.logHighlighterFactory");

  @Nonnull
  private final MainFrame myMainFrame;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final VcsLogColorManager myColorManager;
  @Nonnull
  private final VcsLog myLog;
  @Nonnull
  private final MainVcsLogUiProperties myUiProperties;
  @Nonnull
  private final VcsLogFilterer myFilterer;

  @Nonnull
  private final Collection<VcsLogListener> myLogListeners = ContainerUtil.newArrayList();
  @Nonnull
  private final VisiblePackChangeListener myVisiblePackChangeListener;
  @Nonnull
  private final VcsLogUiPropertiesImpl.MainVcsLogUiPropertiesListener myPropertiesListener;

  @Nonnull
  private VisiblePack myVisiblePack;

  public VcsLogUiImpl(@Nonnull VcsLogData logData,
                      @Nonnull Project project,
                      @Nonnull VcsLogColorManager manager,
                      @Nonnull MainVcsLogUiProperties uiProperties,
                      @Nonnull VcsLogFilterer filterer) {
    myProject = project;
    myColorManager = manager;
    myUiProperties = uiProperties;
    Disposer.register(logData, this);

    myFilterer = filterer;
    myLog = new VcsLogImpl(logData, this);
    myVisiblePack = VisiblePack.EMPTY;
    myMainFrame = new MainFrame(logData, this, project, uiProperties, myLog, myVisiblePack);

    for (VcsLogHighlighterFactory factory : Extensions.getExtensions(LOG_HIGHLIGHTER_FACTORY_EP, myProject)) {
      getTable().addHighlighter(factory.createHighlighter(logData, this));
    }

    myVisiblePackChangeListener = visiblePack -> UIUtil.invokeLaterIfNeeded(() -> {
      if (!Disposer.isDisposed(this)) {
        setVisiblePack(visiblePack);
      }
    });
    myFilterer.addVisiblePackChangeListener(myVisiblePackChangeListener);

    myPropertiesListener = new MyVcsLogUiPropertiesListener();
    myUiProperties.addChangeListener(myPropertiesListener);
  }

  public void requestFocus() {
    // todo fix selection
    final VcsLogGraphTable graphTable = myMainFrame.getGraphTable();
    if (graphTable.getRowCount() > 0) {
      IdeFocusManager.getInstance(myProject).requestFocus(graphTable, true).doWhenProcessed(() -> graphTable.setRowSelectionInterval(0, 0));
    }
  }

  public void setVisiblePack(@Nonnull VisiblePack pack) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    boolean permGraphChanged = myVisiblePack.getDataPack() != pack.getDataPack();

    myVisiblePack = pack;

    myMainFrame.updateDataPack(myVisiblePack, permGraphChanged);
    myPropertiesListener.onShowLongEdgesChanged();
    fireFilterChangeEvent(myVisiblePack, permGraphChanged);
    repaintUI();
  }

  @Nonnull
  public MainFrame getMainFrame() {
    return myMainFrame;
  }

  public void repaintUI() {
    myMainFrame.getGraphTable().repaint();
  }

  private void performLongAction(@Nonnull final GraphAction graphAction, @Nonnull final String title) {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      final GraphAnswer<Integer> answer = myVisiblePack.getVisibleGraph().getActionController().performAction(graphAction);
      final Runnable updater = answer.getGraphUpdater();
      ApplicationManager.getApplication().invokeLater(() -> {
        assert updater != null : "Action:" +
                                 title +
                                 "\nController: " +
                                 myVisiblePack.getVisibleGraph().getActionController() +
                                 "\nAnswer:" +
                                 answer;
        updater.run();
        getTable().handleAnswer(answer, true);
      });
    }, title, false, null, getMainFrame().getMainComponent());
  }

  public void expandAll() {
    performLongAction(new GraphAction.GraphActionImpl(null, GraphAction.Type.BUTTON_EXPAND),
                      "Expanding " +
                      (myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek
                       ? "merges..."
                       : "linear branches..."));
  }

  public void collapseAll() {
    performLongAction(new GraphAction.GraphActionImpl(null, GraphAction.Type.BUTTON_COLLAPSE),
                      "Collapsing " +
                      (myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek
                       ? "merges..."
                       : "linear branches..."));
  }

  public boolean isShowRootNames() {
    return myUiProperties.get(MainVcsLogUiProperties.SHOW_ROOT_NAMES);
  }

  @Override
  public boolean isHighlighterEnabled(@Nonnull String id) {
    VcsLogHighlighterProperty property = VcsLogHighlighterProperty.get(id);
    return myUiProperties.exists(property) && myUiProperties.get(property);
  }

  @Override
  public boolean areGraphActionsEnabled() {
    return myMainFrame.areGraphActionsEnabled();
  }

  public boolean isCompactReferencesView() {
    return myUiProperties.get(MainVcsLogUiProperties.COMPACT_REFERENCES_VIEW);
  }

  public boolean isShowTagNames() {
    return myUiProperties.get(MainVcsLogUiProperties.SHOW_TAG_NAMES);
  }

  @Nonnull
  public Future<Boolean> jumpToCommit(@Nonnull Hash commitHash, @Nonnull VirtualFile root) {
    SettableFuture<Boolean> future = SettableFuture.create();
    jumpToCommit(commitHash, root, future);
    return future;
  }

  public void jumpToCommit(@Nonnull Hash commitHash, @Nonnull VirtualFile root, @Nonnull SettableFuture<Boolean> future) {
    jumpTo(commitHash, (model, hash) -> model.getRowOfCommit(hash, root), future);
  }

  public void jumpToCommitByPartOfHash(@Nonnull String commitHash, @Nonnull SettableFuture<Boolean> future) {
    jumpTo(commitHash, GraphTableModel::getRowOfCommitByPartOfHash, future);
  }

  private <T> void jumpTo(@Nonnull final T commitId,
                          @Nonnull final PairFunction<GraphTableModel, T, Integer> rowGetter,
                          @Nonnull final SettableFuture<Boolean> future) {
    if (future.isCancelled()) return;

    GraphTableModel model = getTable().getModel();

    int row = rowGetter.fun(model, commitId);
    if (row >= 0) {
      myMainFrame.getGraphTable().jumpToRow(row);
      future.set(true);
    }
    else if (model.canRequestMore()) {
      model.requestToLoadMore(() -> jumpTo(commitId, rowGetter, future));
    }
    else if (!myVisiblePack.isFull()) {
      invokeOnChange(() -> jumpTo(commitId, rowGetter, future));
    }
    else {
      commitNotFound(commitId.toString());
      future.set(false);
    }
  }

  private void showMessage(@Nonnull MessageType messageType, @Nonnull String message) {
    LOG.info(message);
    VcsBalloonProblemNotifier.showOverChangesView(myProject, message, messageType);
  }

  private void commitNotFound(@Nonnull String commitHash) {
    if (myMainFrame.getFilterUi().getFilters().isEmpty()) {
      showMessage(MessageType.WARNING, "Commit " + commitHash + " not found");
    }
    else {
      showMessage(MessageType.WARNING, "Commit " + commitHash + " doesn't exist or doesn't match the active filters");
    }
  }

  @Override
  public boolean isMultipleRoots() {
    return myColorManager.isMultipleRoots(); // somewhy color manager knows about this
  }

  @Nonnull
  public VcsLogColorManager getColorManager() {
    return myColorManager;
  }

  public void applyFiltersAndUpdateUi(@Nonnull VcsLogFilterCollection filters) {
    myFilterer.onFiltersChange(filters);
  }

  @Nonnull
  public VcsLogFilterer getFilterer() {
    return myFilterer;
  }

  @Nonnull
  public VcsLogGraphTable getTable() {
    return myMainFrame.getGraphTable();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public JComponent getToolbar() {
    return myMainFrame.getToolbar();
  }

  @Nonnull
  public VcsLog getVcsLog() {
    return myLog;
  }

  @Nonnull
  @Override
  public VcsLogFilterUi getFilterUi() {
    return myMainFrame.getFilterUi();
  }

  @Override
  @Nonnull
  public VisiblePack getDataPack() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myVisiblePack;
  }

  @Override
  public void addLogListener(@Nonnull VcsLogListener listener) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myLogListeners.add(listener);
  }

  @Override
  public void removeLogListener(@Nonnull VcsLogListener listener) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myLogListeners.remove(listener);
  }

  private void fireFilterChangeEvent(@Nonnull VisiblePack visiblePack, boolean refresh) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Collection<VcsLogListener> logListeners = new ArrayList<>(myLogListeners);

    for (VcsLogListener listener : logListeners) {
      listener.onChange(visiblePack, refresh);
    }
  }

  public void invokeOnChange(@Nonnull final Runnable runnable) {
    addLogListener(new VcsLogListener() {
      @Override
      public void onChange(@Nonnull VcsLogDataPack dataPack, boolean refreshHappened) {
        runnable.run();
        removeLogListener(this);
      }
    });
  }

  @Override
  public void dispose() {
    myUiProperties.removeChangeListener(myPropertiesListener);
    myFilterer.removeVisiblePackChangeListener(myVisiblePackChangeListener);
    getTable().removeAllHighlighters();
    myVisiblePack = VisiblePack.EMPTY;
  }

  public MainVcsLogUiProperties getProperties() {
    return myUiProperties;
  }

  private class MyVcsLogUiPropertiesListener extends VcsLogUiPropertiesImpl.MainVcsLogUiPropertiesListener {
    @Override
    public void onShowDetailsChanged() {
      myMainFrame.showDetails(myUiProperties.get(MainVcsLogUiProperties.SHOW_DETAILS));
    }

    @Override
    public void onShowLongEdgesChanged() {
      myVisiblePack.getVisibleGraph().getActionController().setLongEdgesHidden(!myUiProperties.get(MainVcsLogUiProperties.SHOW_LONG_EDGES));
    }

    @Override
    public void onBekChanged() {
      myFilterer.onSortTypeChange(myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE));
    }

    @Override
    public void onShowRootNamesChanged() {
      myMainFrame.getGraphTable().rootColumnUpdated();
    }

    @Override
    public void onHighlighterChanged() {
      repaintUI();
    }

    @Override
    public void onCompactReferencesViewChanged() {
      myMainFrame.getGraphTable().setCompactReferencesView(myUiProperties.get(MainVcsLogUiProperties.COMPACT_REFERENCES_VIEW));
    }

    @Override
    public void onShowTagNamesChanged() {
      myMainFrame.getGraphTable().setShowTagNames(myUiProperties.get(MainVcsLogUiProperties.SHOW_TAG_NAMES));
    }

    @Override
    public void onTextFilterSettingsChanged() {
      applyFiltersAndUpdateUi(myMainFrame.getFilterUi().getFilters());
    }
  }
}
