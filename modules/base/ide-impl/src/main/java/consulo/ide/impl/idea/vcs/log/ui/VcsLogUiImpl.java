package consulo.ide.impl.idea.vcs.log.ui;

import com.google.common.util.concurrent.SettableFuture;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.vcs.log.data.*;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties.VcsLogHighlighterProperty;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogImpl;
import consulo.ide.impl.idea.vcs.log.ui.frame.MainFrame;
import consulo.ide.impl.idea.vcs.log.ui.frame.VcsLogGraphTable;
import consulo.ide.impl.idea.vcs.log.ui.tables.GraphTableModel;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.event.VcsLogListener;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import consulo.versionControlSystem.log.graph.action.GraphAnswer;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

public class VcsLogUiImpl implements VcsLogUi, Disposable {
    private static final Logger LOG = Logger.getInstance(VcsLogUiImpl.class);

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
    private final Collection<VcsLogListener> myLogListeners = new ArrayList<>();
    @Nonnull
    private final VisiblePackChangeListener myVisiblePackChangeListener;
    @Nonnull
    private final VcsLogUiPropertiesImpl.MainVcsLogUiPropertiesListener myPropertiesListener;

    @Nonnull
    private volatile VisiblePack myVisiblePack = VisiblePack.EMPTY;

    public VcsLogUiImpl(
        @Nonnull VcsLogDataImpl logData,
        @Nonnull Project project,
        @Nonnull VcsLogColorManager manager,
        @Nonnull MainVcsLogUiProperties uiProperties,
        @Nonnull VcsLogFilterer filterer
    ) {
        myProject = project;
        myColorManager = manager;
        myUiProperties = uiProperties;
        Disposer.register(logData, this);

        myFilterer = filterer;
        myLog = new VcsLogImpl(logData, this);
        myVisiblePack = VisiblePack.EMPTY;
        myMainFrame = new MainFrame(logData, this, project, uiProperties, myLog, myVisiblePack);

        project.getExtensionPoint(VcsLogHighlighterFactory.class)
            .forEach(factory -> getTable().addHighlighter(factory.createHighlighter(logData, this)));

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
        VcsLogGraphTable graphTable = myMainFrame.getGraphTable();
        if (graphTable.getRowCount() > 0) {
            ProjectIdeFocusManager.getInstance(myProject)
                .requestFocus(graphTable, true)
                .doWhenProcessed(() -> graphTable.setRowSelectionInterval(0, 0));
        }
    }

    @RequiredUIAccess
    public void setVisiblePack(@Nonnull VisiblePack pack) {
        UIAccess.assertIsUIThread();

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

    private void performLongAction(@Nonnull GraphAction graphAction, @Nonnull LocalizeValue title) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> {
                GraphAnswer<Integer> answer = myVisiblePack.getVisibleGraph().getActionController().performAction(graphAction);
                Runnable updater = answer.getGraphUpdater();
                myProject.getApplication().invokeLater(() -> {
                    assert updater != null
                        : "Action:" + title +
                        "\nController: " + myVisiblePack.getVisibleGraph().getActionController() +
                        "\nAnswer:" + answer;
                    updater.run();
                    getTable().handleAnswer(answer, true);
                });
            },
            title,
            false,
            null,
            getMainFrame().getMainComponent()
        );
    }

    public void expandAll() {
        performLongAction(
            new GraphAction.GraphActionImpl(null, GraphAction.Type.BUTTON_EXPAND),
            myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek
                ? VersionControlSystemLogLocalize.processExpandingMerges()
                : VersionControlSystemLogLocalize.processExpandingLinearBranches()
        );
    }

    public void collapseAll() {
        performLongAction(
            new GraphAction.GraphActionImpl(null, GraphAction.Type.BUTTON_COLLAPSE),
            myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek
                ? VersionControlSystemLogLocalize.processCollapsingMerges()
                : VersionControlSystemLogLocalize.processCollapsingLinearBranches()
        );
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
    @RequiredUIAccess
    public Future<Boolean> jumpToCommit(@Nonnull Hash commitHash, @Nonnull VirtualFile root) {
        SettableFuture<Boolean> future = SettableFuture.create();
        jumpToCommit(commitHash, root, future);
        return future;
    }

    @RequiredUIAccess
    public void jumpToCommit(@Nonnull Hash commitHash, @Nonnull VirtualFile root, @Nonnull SettableFuture<Boolean> future) {
        jumpTo(commitHash, (model, hash) -> model.getRowOfCommit(hash, root), future);
    }

    @RequiredUIAccess
    public void jumpToCommitByPartOfHash(@Nonnull String commitHash, @Nonnull SettableFuture<Boolean> future) {
        jumpTo(commitHash, GraphTableModel::getRowOfCommitByPartOfHash, future);
    }

    @RequiredUIAccess
    private <T> void jumpTo(
        @Nonnull T commitId,
        @Nonnull BiFunction<GraphTableModel, T, Integer> rowGetter,
        @Nonnull SettableFuture<Boolean> future
    ) {
        if (future.isCancelled()) {
            return;
        }

        GraphTableModel model = getTable().getModel();

        int row = rowGetter.apply(model, commitId);
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

    private void showMessage(@Nonnull NotificationType messageType, @Nonnull String message) {
        LOG.info(message);
        VcsBalloonProblemNotifier.showOverChangesView(myProject, message, messageType);
    }

    private void commitNotFound(@Nonnull String commitHash) {
        if (myMainFrame.getFilterUi().getFilters().isEmpty()) {
            showMessage(NotificationType.WARNING, "Commit " + commitHash + " not found");
        }
        else {
            showMessage(NotificationType.WARNING, "Commit " + commitHash + " doesn't exist or doesn't match the active filters");
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
        return myVisiblePack;
    }

    @Override
    @RequiredUIAccess
    public void addLogListener(@Nonnull VcsLogListener listener) {
        UIAccess.assertIsUIThread();
        myLogListeners.add(listener);
    }

    @Override
    @RequiredUIAccess
    public void removeLogListener(@Nonnull VcsLogListener listener) {
        UIAccess.assertIsUIThread();
        myLogListeners.remove(listener);
    }

    @RequiredUIAccess
    private void fireFilterChangeEvent(@Nonnull VisiblePack visiblePack, boolean refresh) {
        UIAccess.assertIsUIThread();
        Collection<VcsLogListener> logListeners = new ArrayList<>(myLogListeners);

        for (VcsLogListener listener : logListeners) {
            listener.onChange(visiblePack, refresh);
        }
    }

    @RequiredUIAccess
    public void invokeOnChange(@Nonnull final Runnable runnable) {
        addLogListener(new VcsLogListener() {
            @Override
            @RequiredUIAccess
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
            myVisiblePack.getVisibleGraph()
                .getActionController()
                .setLongEdgesHidden(!myUiProperties.get(MainVcsLogUiProperties.SHOW_LONG_EDGES));
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
