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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.OptionButtonItem;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.action.OptionButtonAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.CommitResultHandler;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.BaseCheckinHandlerFactory;
import consulo.versionControlSystem.checkin.BeforeCheckinDialogHandler;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.impl.internal.change.action.RollbackDialogAction;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeTreeItem;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeGroupKind;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesLegend;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeItem;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeSnapshot;
import consulo.versionControlSystem.impl.internal.change.ui.ChangesTreeSnapshotBuilder;
import consulo.versionControlSystem.impl.internal.change.ui.FileTreeItem;
import consulo.versionControlSystem.impl.internal.change.ui.GroupByDirectoryToggleAction;
import consulo.versionControlSystem.impl.internal.change.ui.UnifiedChangesTree;
import consulo.versionControlSystem.internal.CheckinHandlersManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitChangeListDialog extends DialogDescriptor implements UnifiedCommitWorkflowUi {
    private static final Logger LOG = Logger.getInstance(UnifiedCommitChangeListDialog.class);

    private static final int TREE_PROPORTION = 60;
    private static final int LEFT_PROPORTION = 70;

    private static final String SHOW_DIFF_ACTION = "ChangesView.Diff";
    private static final String MOVE_ACTION = "ChangesView.Move";
    private static final String MESSAGE_ACTION_GROUP = "Vcs.MessageActionGroup";

    private static final int NEW = 0;
    private static final int MODIFIED = 1;
    private static final int DELETED = 2;
    private static final int UNVERSIONED = 3;
    private static final int LEGEND_SIZE = 4;

    private final Project myProject;
    private LocalChangeList myChangeList;
    private final Collection<?> myInitiallyIncluded;
    private final @Nullable CommitExecutor myExecutor;
    private final List<CommitExecutor> myExecutors;
    private final @Nullable AbstractVcs mySingleVcs;
    private final @Nullable String myComment;
    private final @Nullable CommitResultHandler myResultHandler;
    private final LocalizeValue myCommitActionName;

    private final UnifiedCommitOptions myOptions = new UnifiedCommitOptions();
    private @Nullable ChangesLegend myLegend;
    private boolean myShowFlatten = true;

    private @Nullable UnifiedChangesTree myTree;
    private @Nullable EditorBox myMessageBox;
    private @Nullable UnifiedCommitStatus myStatus;
    private @Nullable UnifiedCommitWorkflowHandler myHandler;
    private @Nullable Dialog myDialog;
    private boolean myClosedByCommit;
    private boolean myClosed;
    private boolean myExecutorRunning;

    private UnifiedCommitChangeListDialog(Project project,
                                          LocalChangeList changeList,
                                          Collection<?> included,
                                          @Nullable CommitExecutor executor,
                                          List<CommitExecutor> executors,
                                          @Nullable AbstractVcs singleVcs,
                                          @Nullable String comment,
                                          @Nullable CommitResultHandler resultHandler) {
        super(VcsLocalize.commitDialogTitle());
        myProject = project;
        myChangeList = changeList;
        myInitiallyIncluded = included;
        myExecutor = executor;
        myExecutors = executors;
        mySingleVcs = singleVcs;
        myComment = comment;
        myResultHandler = resultHandler;
        myCommitActionName = executor != null ? executor.getActionText() : commitActionName(project, singleVcs);
    }

    @RequiredUIAccess
    public static CompletableFuture<Boolean> show(Project project,
                                                  Collection<?> included,
                                                  @Nullable LocalChangeList initialSelection,
                                                  @Nullable CommitExecutor executor) {
        List<CommitExecutor> executors = executor != null ? List.of(executor) : collectExecutors(project);
        return show(project, included, initialSelection, executors, executor == null, null, null, null, false);
    }

    /**
     * Shows the commit dialog. When {@code showVcsCommit} is false the first executor becomes the main action.
     */
    @RequiredUIAccess
    public static CompletableFuture<Boolean> show(Project project,
                                                  Collection<?> included,
                                                  @Nullable LocalChangeList initialSelection,
                                                  List<CommitExecutor> executors,
                                                  boolean showVcsCommit,
                                                  @Nullable AbstractVcs singleVcs,
                                                  @Nullable String comment,
                                                  @Nullable CommitResultHandler resultHandler,
                                                  boolean cancelIfNoChanges) {
        if (cancelIfNoChanges && included.isEmpty()) {
            return MessageBoxes.okInfo(VcsLocalize.commitDialogNoChangesDetectedText())
                .title(VcsLocalize.commitDialogNoChangesDetectedTitle())
                .showAsync()
                .thenApply(value -> Boolean.FALSE);
        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        LocalChangeList changeList = initialSelection != null ? initialSelection : changeListManager.getDefaultChangeList();

        boolean vcsCommit = showVcsCommit || executors.isEmpty();
        CommitExecutor mainExecutor = vcsCommit ? null : executors.get(0);
        List<CommitExecutor> optionExecutors = vcsCommit ? List.copyOf(executors) : List.copyOf(executors.subList(1, executors.size()));

        List<Change> changes = new ArrayList<>();
        for (Object item : included) {
            if (item instanceof Change change) {
                changes.add(change);
            }
        }
        for (BaseCheckinHandlerFactory factory : CheckinHandlersManager.getInstance()
            .getRegisteredCheckinHandlerFactories(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss())) {
            BeforeCheckinDialogHandler handler = factory.createSystemReadyHandler(project);
            if (handler != null && !handler.beforeCommitDialogShown(project, changes, executors, vcsCommit)) {
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }
        }

        UnifiedCommitChangeListDialog descriptor = new UnifiedCommitChangeListDialog(
            project,
            changeList,
            included,
            mainExecutor,
            optionExecutors,
            singleVcs,
            comment,
            resultHandler
        );
        Dialog dialog = project.getApplication().getInstance(DialogService.class).build(descriptor);
        descriptor.myDialog = dialog;

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        dialog.showAsync().whenComplete((value, error) -> result.complete(descriptor.myClosedByCommit));
        return result;
    }

    private static List<CommitExecutor> collectExecutors(Project project) {
        List<CommitExecutor> executors = new ArrayList<>();
        for (AbstractVcs vcs : ProjectLevelVcsManager.getInstance(project).getAllActiveVcss()) {
            executors.addAll(vcs.getCommitExecutors());
        }
        executors.addAll(ChangeListManager.getInstance(project).getRegisteredExecutors());
        return executors;
    }

    public static List<CommitExecutor> collectExecutors(Project project, Collection<Change> changes) {
        List<CommitExecutor> executors = new ArrayList<>();
        for (AbstractVcs vcs : ChangesUtil.getAffectedVcses(changes, project)) {
            executors.addAll(vcs.getCommitExecutors());
        }
        executors.addAll(ChangeListManager.getInstance(project).getRegisteredExecutors());
        return executors;
    }

    private static LocalizeValue commitActionName(Project project, @Nullable AbstractVcs singleVcs) {
        AbstractVcs[] vcses = singleVcs != null ? new AbstractVcs[]{singleVcs} : ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
        if (vcses.length == 1) {
            CheckinEnvironment environment = vcses[0].getCheckinEnvironment();
            if (environment != null) {
                return environment.getCheckinOperationName();
            }
        }
        return VcsLocalize.commitDialogDefaultCommitOperationName();
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        ChangesLegend legend = createLegend();
        myLegend = legend;

        UnifiedChangesTree tree = new UnifiedChangesTree(myProject, true, uiDisposable);
        tree.setAutoIncludeDefaultList(false);
        tree.addInclusionListener(this::updateLegend);
        myTree = tree;

        tree.setSnapshot(buildSnapshot());
        tree.setIncluded(myInitiallyIncluded);

        UnifiedCommitWorkflow workflow = new UnifiedCommitWorkflow(
            myProject,
            myProject.getApplication().getInstance(ProgressBuilderFactory.class),
            myExecutors,
            true,
            mySingleVcs,
            myResultHandler
        );
        UnifiedCommitWorkflowHandler handler = new UnifiedCommitWorkflowHandler(workflow, this, myCommitActionName);
        myHandler = handler;

        EditorBox messageBox = myProject.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(myProject)
            .multiline()
            .placeholder(VcsLocalize.commitPanelMessagePlaceholder())
            .build();
        myMessageBox = messageBox;

        handler.activate();
        if (myComment != null) {
            messageBox.setValue(myComment);
        }

        DockLayout left = DockLayout.create();
        left.putUserData(UiDataProvider.KEY, sink -> {
            tree.uiDataSnapshot(sink);
            sink.set(Refreshable.PANEL_KEY, workflow.getProjectPanel());
        });
        left.top(createHeader(tree, left));

        DockLayout treePanel = DockLayout.create();
        treePanel.center(ScrollableLayout.create(tree.getComponent()));
        treePanel.bottom(DockLayout.create().right(legend.getComponent()));

        ActionToolbar messageToolbar = createToolbar(MESSAGE_ACTION_GROUP, left, ActionToolbar.Style.VERTICAL);

        DockLayout messagePanel = DockLayout.create();
        messagePanel.center(messageBox);
        messagePanel.right(messageToolbar.getUIComponent());

        TwoComponentSplitLayout treeAndMessage = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        treeAndMessage.setProportion(TREE_PROPORTION);
        treeAndMessage.setFirstComponent(treePanel);
        treeAndMessage.setSecondComponent(LabeledLayout.create(VcsLocalize.commitDialogMessageTitle(), messagePanel));
        left.center(treeAndMessage);

        UnifiedCommitStatus status = new UnifiedCommitStatus(myProject);
        myStatus = status;

        myOptions.fill(workflow, myCommitActionName, true, uiDisposable);

        DockLayout right = DockLayout.create();
        right.top(myOptions.getComponent());
        right.bottom(status.getComponent());
        right.paddingBuilder().leftSet(Space.X_LARGE).rightSet(Space.MEDIUM).topSet(Space.SMALL).apply();

        TwoComponentSplitLayout root = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        root.setProportion(LEFT_PROPORTION);
        root.setFirstComponent(left);
        root.setSecondComponent(ScrollableLayout.create(right));

        ChangeListListener listener = new ChangeListListener() {
            @Override
            public void changeListUpdateDone() {
                scheduleRefresh();
            }
        };
        ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        changeListManager.addChangeListListener(listener);
        Disposer.register(uiDisposable, () -> changeListManager.removeChangeListListener(listener));

        updateLegend();
        return root;
    }

    @RequiredUIAccess
    private Component createHeader(UnifiedChangesTree tree, Component target) {
        DefaultActionGroup group = new DefaultActionGroup();
        ActionManager actionManager = ActionManager.getInstance();
        if (actionManager.getAction(SHOW_DIFF_ACTION) instanceof AnAction showDiff) {
            group.add(showDiff);
        }
        group.add(new RefreshChangesAction());
        group.add(new RollbackDialogAction());
        if (actionManager.getAction(MOVE_ACTION) instanceof AnAction move) {
            group.add(move);
        }
        group.addSeparator();
        group.add(new ShowUnversionedFilesAction());
        group.add(new GroupByDirectoryToggleAction(() -> !myShowFlatten, grouped -> {
            myShowFlatten = !grouped;
            scheduleRefresh();
        }));
        TreeExpander expander = TreeExpander.of(tree.getComponent());
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        group.add(commonActionsManager.createExpandAllAction(expander));
        group.add(commonActionsManager.createCollapseAllAction(expander));

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(target);

        List<LocalChangeList> changeLists = ChangeListManager.getInstance(myProject).getChangeListsCopy();
        ComboBox<LocalChangeList> chooser = ComboBox.create(changeLists);
        chooser.setRenderer((presentation, item) -> {
            LocalChangeList value = item.getValue();
            if (value != null) {
                presentation.append(
                    LocalizeValue.of(value.getName()),
                    value.isDefault() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
                );
            }
        });
        chooser.setValue(myChangeList);
        chooser.setEnabled(changeLists.size() > 1);
        chooser.addValueListener(event -> {
            LocalChangeList newList = event.getValue();
            if (newList != null && !newList.equals(myChangeList)) {
                changeListSelected(newList);
            }
        });

        Label chooserLabel = Label.create(VcsLocalize.commitDialogChangelistLabel());
        chooserLabel.setTarget(chooser);

        DockLayout header = DockLayout.create();
        header.left(toolbar.getUIComponent());
        header.right(HorizontalLayout.create().add(chooserLabel).add(chooser));
        return header;
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(String groupId, Component target, ActionToolbar.Style style) {
        ActionGroup group = ActionManager.getInstance().getAction(groupId) instanceof ActionGroup actionGroup
            ? actionGroup
            : ActionGroup.newImmutableBuilder().build();
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, style);
        toolbar.setTargetUIComponent(target);
        return toolbar;
    }

    @RequiredUIAccess
    private ChangesLegend createLegend() {
        FileStatus[] statuses = {FileStatus.ADDED, FileStatus.MODIFIED, FileStatus.DELETED, FileStatus.UNKNOWN};

        ChangesLegend legend = new ChangesLegend();
        for (int i = 0; i < LEGEND_SIZE; i++) {
            legend.addEntry(legendTitle(i), statuses[i]);
        }
        return legend;
    }

    @RequiredUIAccess
    private void changeListSelected(LocalChangeList newList) {
        LocalChangeList oldList = myChangeList;
        myChangeList = newList;

        UnifiedChangesTree tree = myTree;
        if (tree != null) {
            tree.setSnapshot(buildSnapshot());
            tree.setIncluded(newList.getChanges());
        }

        UnifiedCommitWorkflowHandler handler = myHandler;
        if (handler != null) {
            handler.changeListChanged(oldList, newList);
        }
    }

    private ChangesTreeSnapshot buildSnapshot() {
        LocalChangeList changeList = ChangeListManager.getInstance(myProject).findChangeList(myChangeList.getName());
        if (changeList != null) {
            myChangeList = changeList;
        }

        ChangesTreeSnapshotBuilder builder = new ChangesTreeSnapshotBuilder(myProject, myShowFlatten)
            .changeLists(List.of(myChangeList));
        if (VcsConfiguration.getInstance(myProject).SHOW_UNVERSIONED_FILES_WHILE_COMMIT) {
            builder.files(ChangesTreeGroupKind.UNVERSIONED, ChangeListManagerImpl.getInstanceImpl(myProject).getUnversionedFiles());
        }
        return builder.build();
    }

    private void scheduleRefresh() {
        UIAccess uiAccess = myProject.getUIAccess();
        if (uiAccess.isValid()) {
            refreshTree(uiAccess);
        }
    }

    private CompletableFuture<?> refreshTree(UIAccess uiAccess) {
        return CompletableFuture.supplyAsync(this::buildSnapshot, AppExecutorUtil.getAppExecutorService())
            .thenComposeAsync(snapshot -> {
                UnifiedChangesTree tree = myTree;
                if (tree == null || myClosed) {
                    return CompletableFuture.completedFuture(null);
                }
                return tree.setSnapshot(snapshot);
            }, uiAccess)
            .whenComplete((result, error) -> {
                if (error != null) {
                    LOG.error(error);
                }
            });
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> refreshChangesBeforeCommit() {
        return refreshTree(UIAccess.current());
    }

    @RequiredUIAccess
    private void updateLegend() {
        UnifiedChangesTree tree = myTree;
        ChangesLegend legend = myLegend;
        if (tree == null || legend == null) {
            return;
        }

        ChangesTreeSnapshot snapshot = tree.getSnapshot();
        int[] total = new int[LEGEND_SIZE];
        for (ChangesTreeItem root : snapshot.getRoots()) {
            for (ChangesTreeItem leaf : snapshot.collectLeaves(root)) {
                if (leaf instanceof ChangeTreeItem changeItem) {
                    total[legendIndex(changeItem.change())]++;
                }
                else if (leaf instanceof FileTreeItem fileItem && fileItem.kind() == ChangesTreeGroupKind.UNVERSIONED) {
                    total[UNVERSIONED]++;
                }
            }
        }

        int[] included = new int[LEGEND_SIZE];
        for (Change change : tree.getIncludedChanges()) {
            included[legendIndex(change)]++;
        }
        included[UNVERSIONED] = tree.getIncludedUnversionedFiles().size();

        for (int i = 0; i < LEGEND_SIZE; i++) {
            legend.setCount(i, total[i], included[i]);
        }
    }

    private static LocalizeValue legendTitle(int index) {
        return switch (index) {
            case NEW -> VcsLocalize.commitLegendNew();
            case MODIFIED -> VcsLocalize.commitLegendModified();
            case DELETED -> VcsLocalize.commitLegendDeleted();
            default -> VcsLocalize.commitLegendUnversioned();
        };
    }

    private static int legendIndex(Change change) {
        return switch (change.getType()) {
            case NEW -> NEW;
            case DELETED -> DELETED;
            case MODIFICATION, MOVED -> MODIFIED;
        };
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(90, 40);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "UnifiedCommitChangeListDialog";
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myMessageBox;
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        AnAction commit = new OptionButtonAction(myCommitActionName, true) {
            @Override
            protected List<OptionButtonItem> getOptions() {
                List<OptionButtonItem> options = new ArrayList<>();
                for (CommitExecutor executor : myExecutors) {
                    options.add(OptionButtonItem.of(executor.getActionText(), event -> commit(executor)));
                }
                return options;
            }

            @RequiredUIAccess
            @Override
            protected void performMain() {
                commit(myExecutor);
            }
        };

        DialogCancelAction cancel = new DialogCancelAction();
        return inverseOrder ? new AnAction[]{cancel, commit} : new AnAction[]{commit, cancel};
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (action instanceof DialogCancelAction) {
            UnifiedCommitWorkflowHandler handler = myHandler;
            if (handler != null) {
                handler.cancelled();
            }
        }
        return true;
    }

    @RequiredUIAccess
    private void commit(@Nullable CommitExecutor executor) {
        UnifiedCommitWorkflowHandler handler = myHandler;
        if (handler == null) {
            return;
        }

        myOptions.saveState();
        myExecutorRunning = executor != null;

        UIAccess uiAccess = UIAccess.current();
        handler.executorCalled(executor).whenCompleteAsync((committed, error) -> {
            if (error != null) {
                LOG.error(error);
            }
        }, uiAccess);
    }

    @Override
    public LocalChangeList getChangeList() {
        return myChangeList;
    }

    @Override
    public List<Change> getDisplayedChanges() {
        return List.copyOf(myChangeList.getChanges());
    }

    @Override
    public List<Change> getIncludedChanges() {
        UnifiedChangesTree tree = myTree;
        return tree == null ? List.of() : tree.getIncludedChanges();
    }

    @Override
    public List<VirtualFile> getIncludedUnversionedFiles() {
        UnifiedChangesTree tree = myTree;
        return tree == null ? List.of() : tree.getIncludedUnversionedFiles();
    }

    @RequiredUIAccess
    @Override
    public void includeChanges(Collection<Change> changes) {
        UnifiedChangesTree tree = myTree;
        if (tree != null) {
            tree.includeChanges(changes);
        }
    }

    @Override
    public String getCommitMessage() {
        EditorBox box = myMessageBox;
        String value = box == null ? null : box.getValue();
        return value == null ? "" : value;
    }

    @RequiredUIAccess
    @Override
    public void setCommitMessage(String commitMessage) {
        EditorBox box = myMessageBox;
        if (box != null) {
            box.setValue(commitMessage);
        }
    }

    @RequiredUIAccess
    @Override
    public void startBeforeCommitChecks() {
        UnifiedCommitStatus status = myStatus;
        if (status != null) {
            status.showRunning();
        }
    }

    @RequiredUIAccess
    @Override
    public void endBeforeCommitChecks(CheckinHandler.ReturnResult result, @Nullable CommitProblem problem) {
        UnifiedCommitStatus status = myStatus;
        if (status != null) {
            if (problem != null) {
                status.showProblem(problem.getText(), problem);
            }
            else {
                status.hide();
            }
        }

        if (result == CheckinHandler.ReturnResult.COMMIT && !myExecutorRunning) {
            deactivate(true);
        }
    }

    @RequiredUIAccess
    @Override
    public void deactivate(boolean committed) {
        Dialog dialog = myDialog;
        if (dialog == null || myClosed) {
            return;
        }
        myClosed = true;

        if (committed) {
            myClosedByCommit = true;
            dialog.doOkAction(DialogValue.OK_VALUE);
        }
        else {
            dialog.doCancelAction();
        }
    }

    private class RefreshChangesAction extends DumbAwareAction {
        private RefreshChangesAction() {
            super(VcsLocalize.commitDialogRefreshChanges(), LocalizeValue.empty(), PlatformIconGroup.actionsRefresh());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            scheduleRefresh();
        }
    }

    private class ShowUnversionedFilesAction extends DumbAwareToggleAction {
        private ShowUnversionedFilesAction() {
            super(VcsLocalize.commitDialogShowUnversionedFiles(), LocalizeValue.empty(), PlatformIconGroup.actionsTogglevisibility());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return VcsConfiguration.getInstance(myProject).SHOW_UNVERSIONED_FILES_WHILE_COMMIT;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            VcsConfiguration.getInstance(myProject).SHOW_UNVERSIONED_FILES_WHILE_COMMIT = state;
            scheduleRefresh();
        }
    }
}
