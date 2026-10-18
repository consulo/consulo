/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.log.impl.internal.ui;

import com.google.common.util.concurrent.SettableFuture;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.event.VcsLogListener;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import consulo.versionControlSystem.log.graph.action.GraphAnswer;
import consulo.versionControlSystem.log.impl.internal.VcsLogImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogUiPropertiesImpl;
import consulo.versionControlSystem.log.impl.internal.data.VisiblePack;
import consulo.versionControlSystem.log.impl.internal.data.VisiblePackChangeListener;
import consulo.versionControlSystem.log.impl.internal.ui.filter.VcsLogClassicFilterUi;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public abstract class AbstractVcsLogUi implements VcsLogUi, Disposable {
    private static final Logger LOG = Logger.getInstance(AbstractVcsLogUi.class);

    private final String myId;
    protected final Project myProject;
    protected final VcsLogDataImpl myLogData;
    protected final VcsLogColorManager myColorManager;
    protected final MainVcsLogUiProperties myUiProperties;
    protected final VcsLogFilterer myFilterer;
    protected final VcsLog myLog;

    private final Collection<VcsLogListener> myLogListeners = new ArrayList<>();
    private final VisiblePackChangeListener myVisiblePackChangeListener;
    private final VcsLogUiPropertiesImpl.MainVcsLogUiPropertiesListener myPropertiesListener;

    protected volatile VisiblePack myVisiblePack = VisiblePack.EMPTY;

    protected AbstractVcsLogUi(
        String id,
        VcsLogDataImpl logData,
        Project project,
        VcsLogColorManager manager,
        MainVcsLogUiProperties uiProperties,
        VcsLogFilterer filterer
    ) {
        myId = id;
        myProject = project;
        myLogData = logData;
        myColorManager = manager;
        myUiProperties = uiProperties;
        myFilterer = filterer;
        Disposer.register(logData, this);

        myLog = new VcsLogImpl(logData, this);

        myVisiblePackChangeListener = visiblePack -> myProject.getUIAccess().giveIfNeed(() -> {
            if (!Disposer.isDisposed(this)) {
                setVisiblePack(visiblePack);
            }
        });

        myPropertiesListener = new MyVcsLogUiPropertiesListener();
    }

    @Override
    public String getId() {
        return myId;
    }

    protected final void startListening() {
        myFilterer.addVisiblePackChangeListener(myVisiblePackChangeListener);
        myUiProperties.addChangeListener(myPropertiesListener);
    }

    public abstract VcsLogCommitModel getCommitModel();

    public abstract List<Integer> getSelectedRows();

    @RequiredUIAccess
    public abstract Component createPanel(VcsLogManager manager);

    @RequiredUIAccess
    public abstract void requestFocus();

    @RequiredUIAccess
    protected abstract void onVisiblePackUpdated(boolean permGraphChanged);

    @RequiredUIAccess
    protected abstract void jumpToRow(int row);

    @RequiredUIAccess
    protected abstract void handleAnswer(GraphAnswer<Integer> answer);

    public abstract void repaintUI();

    @Override
    public abstract VcsLogClassicFilterUi getFilterUi();

    @RequiredUIAccess
    protected void onShowDetailsChanged() {
    }

    @RequiredUIAccess
    protected void onShowRootNamesChanged() {
    }

    @RequiredUIAccess
    protected void onCompactReferencesViewChanged() {
    }

    @RequiredUIAccess
    protected void onShowTagNamesChanged() {
    }

    @RequiredUIAccess
    public void setVisiblePack(VisiblePack pack) {
        UIAccess.assertIsUIThread();

        boolean permGraphChanged = myVisiblePack.getDataPack() != pack.getDataPack();

        myVisiblePack = pack;

        onVisiblePackUpdated(permGraphChanged);
        myPropertiesListener.onShowLongEdgesChanged();
        fireFilterChangeEvent(myVisiblePack, permGraphChanged);
        repaintUI();
    }

    @RequiredUIAccess
    private void performLongAction(GraphAction graphAction, LocalizeValue title) {
        UIAccess uiAccess = UIAccess.current();
        myProject.getApplication().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(myProject, title)
            .modal()
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, GraphAnswer<Integer>>apply((input, continuation) ->
                myVisiblePack.getVisibleGraph().getActionController().performAction(graphAction)
            )))
            .whenCompleteAsync((answer, error) -> {
                if (error != null || answer == null) {
                    if (error != null) {
                        LOG.error(error);
                    }
                    return;
                }

                Runnable updater = answer.getGraphUpdater();
                if (updater == null) {
                    LOG.error("Action: " + title + "\nController: " + myVisiblePack.getVisibleGraph().getActionController() + "\nAnswer: " + answer);
                    return;
                }
                updater.run();
                handleAnswer(answer);
            }, uiAccess);
    }

    @RequiredUIAccess
    public void expandAll() {
        performLongAction(
            new GraphAction.GraphActionImpl(null, GraphAction.Type.BUTTON_EXPAND),
            myUiProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek
                ? VersionControlSystemLogLocalize.processExpandingMerges()
                : VersionControlSystemLogLocalize.processExpandingLinearBranches()
        );
    }

    @RequiredUIAccess
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
    public boolean isHighlighterEnabled(String id) {
        MainVcsLogUiProperties.VcsLogHighlighterProperty property = MainVcsLogUiProperties.VcsLogHighlighterProperty.get(id);
        return myUiProperties.exists(property) && myUiProperties.get(property);
    }

    @Override
    public boolean areGraphActionsEnabled() {
        return getCommitModel().getRowCount() > 0;
    }

    public boolean isCompactReferencesView() {
        return myUiProperties.get(MainVcsLogUiProperties.COMPACT_REFERENCES_VIEW);
    }

    public boolean isShowTagNames() {
        return myUiProperties.get(MainVcsLogUiProperties.SHOW_TAG_NAMES);
    }

    @RequiredUIAccess
    public Future<Boolean> jumpToCommit(Hash commitHash, VirtualFile root) {
        SettableFuture<Boolean> future = SettableFuture.create();
        jumpToCommit(commitHash, root, future);
        return future;
    }

    @RequiredUIAccess
    public void jumpToCommit(Hash commitHash, VirtualFile root, SettableFuture<Boolean> future) {
        jumpTo(commitHash, (model, hash) -> model.getRowOfCommit(hash, root), future);
    }

    @RequiredUIAccess
    public void jumpToCommitByPartOfHash(String commitHash, SettableFuture<Boolean> future) {
        jumpTo(commitHash, VcsLogCommitModel::getRowOfCommitByPartOfHash, future);
    }

    @RequiredUIAccess
    private <T> void jumpTo(T commitId, BiFunction<VcsLogCommitModel, T, Integer> rowGetter, SettableFuture<Boolean> future) {
        if (future.isCancelled()) {
            return;
        }

        VcsLogCommitModel model = getCommitModel();

        int row = rowGetter.apply(model, commitId);
        if (row >= 0) {
            jumpToRow(row);
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

    private void showMessage(NotificationType messageType, String message) {
        LOG.info(message);
        VcsBalloonProblemNotifier.showOverChangesView(myProject, message, messageType);
    }

    private void commitNotFound(String commitHash) {
        if (getFilterUi().getFilters().isEmpty()) {
            showMessage(NotificationType.WARNING, "Commit " + commitHash + " not found");
        }
        else {
            showMessage(NotificationType.WARNING, "Commit " + commitHash + " doesn't exist or doesn't match the active filters");
        }
    }

    @Override
    public boolean isMultipleRoots() {
        return myColorManager.isMultipleRoots();
    }

    public VcsLogColorManager getColorManager() {
        return myColorManager;
    }

    public void applyFiltersAndUpdateUi(VcsLogFilterCollection filters) {
        myFilterer.onFiltersChange(filters);
    }

    public VcsLogFilterer getFilterer() {
        return myFilterer;
    }

    public VcsLogDataImpl getLogData() {
        return myLogData;
    }

    public Project getProject() {
        return myProject;
    }

    public VcsLog getVcsLog() {
        return myLog;
    }

    @Override
    public VisiblePack getDataPack() {
        return myVisiblePack;
    }

    public MainVcsLogUiProperties getProperties() {
        return myUiProperties;
    }

    @Override
    @RequiredUIAccess
    public void addLogListener(VcsLogListener listener) {
        UIAccess.assertIsUIThread();
        myLogListeners.add(listener);
    }

    @Override
    @RequiredUIAccess
    public void removeLogListener(VcsLogListener listener) {
        UIAccess.assertIsUIThread();
        myLogListeners.remove(listener);
    }

    @RequiredUIAccess
    private void fireFilterChangeEvent(VisiblePack visiblePack, boolean refresh) {
        UIAccess.assertIsUIThread();
        Collection<VcsLogListener> logListeners = new ArrayList<>(myLogListeners);

        for (VcsLogListener listener : logListeners) {
            listener.onChange(visiblePack, refresh);
        }
    }

    @RequiredUIAccess
    public void invokeOnChange(Runnable runnable) {
        addLogListener(new VcsLogListener() {
            @Override
            @RequiredUIAccess
            public void onChange(VcsLogDataPack dataPack, boolean refreshHappened) {
                runnable.run();
                removeLogListener(this);
            }
        });
    }

    @Override
    public void dispose() {
        myUiProperties.removeChangeListener(myPropertiesListener);
        myFilterer.removeVisiblePackChangeListener(myVisiblePackChangeListener);
        myVisiblePack = VisiblePack.EMPTY;
    }

    private class MyVcsLogUiPropertiesListener extends VcsLogUiPropertiesImpl.MainVcsLogUiPropertiesListener {
        @Override
        public void onShowDetailsChanged() {
            AbstractVcsLogUi.this.onShowDetailsChanged();
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
            AbstractVcsLogUi.this.onShowRootNamesChanged();
        }

        @Override
        public void onHighlighterChanged() {
            repaintUI();
        }

        @Override
        public void onCompactReferencesViewChanged() {
            AbstractVcsLogUi.this.onCompactReferencesViewChanged();
        }

        @Override
        public void onShowTagNamesChanged() {
            AbstractVcsLogUi.this.onShowTagNamesChanged();
        }

        @Override
        public void onTextFilterSettingsChanged() {
            applyFiltersAndUpdateUi(getFilterUi().getFilters());
        }
    }
}
