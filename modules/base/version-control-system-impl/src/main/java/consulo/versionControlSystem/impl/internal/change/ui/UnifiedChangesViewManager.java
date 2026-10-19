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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.ChangesViewManager;
import consulo.versionControlSystem.commit.CommitMode;
import consulo.versionControlSystem.commit.CommitModeListener;
import consulo.versionControlSystem.commit.CommitModeManager;
import consulo.versionControlSystem.commit.NonModalCommitMode;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.impl.internal.commit.UnifiedCommitPanel;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.versionControlSystem.internal.RemoteRevisionChangeListener;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@Singleton
@ServiceImpl(profiles = ProjectEx.REGULAR_PROJECT)
@State(name = "ChangesViewManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class UnifiedChangesViewManager implements ChangesViewManager, PersistentStateComponent<UnifiedChangesViewManager.State>, Disposable {
    public static class State {
        public boolean myShowFlatten = true;
        public boolean myShowIgnored;
    }

    private static final long REFRESH_DELAY_MS = 100;

    private final Project myProject;
    private final Application myApplication;
    private final ChangesViewContentI myContentManager;

    private State myState = new State();

    private static final int VCS_TOOL_WINDOW_TREE_PROPORTION = 55;

    private static final int COMMIT_TOOL_WINDOW_TREE_PROPORTION = 60;

    private @Nullable UnifiedChangesTree myTree;
    private @Nullable DockLayout myContentRoot;
    private @Nullable Disposable myContentDisposable;
    private @Nullable UnifiedCommitPanel myCommitPanel;
    private @Nullable Label myStatusLabel;
    private @Nullable ScheduledFuture<?> myRefreshFuture;

    private boolean myCommitPanelShown;
    private boolean myDisposed;

    private final ChangeListListener myListener = new ChangeListListener() {
        @Override
        public void changeListAdded(ChangeList list) {
            scheduleRefresh();
        }

        @Override
        public void changeListRemoved(ChangeList list) {
            scheduleRefresh();
        }

        @Override
        public void changeListRenamed(ChangeList list, String oldName) {
            scheduleRefresh();
        }

        @Override
        public void changesMoved(Collection<Change> changes,
                                 ChangeList fromList,
                                 ChangeList toList) {
            scheduleRefresh();
        }

        @Override
        public void defaultListChanged(ChangeList oldDefaultList,
                                       ChangeList newDefaultList) {
            scheduleRefresh();
        }

        @Override
        public void changeListUpdateDone() {
            scheduleRefresh();
        }
    };

    @Inject
    public UnifiedChangesViewManager(Project project, Application application, ChangesViewContentI contentManager) {
        myProject = project;
        myApplication = application;
        myContentManager = contentManager;
    }

    @RequiredUIAccess
    void projectOpened() {

        ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        changeListManager.addChangeListListener(myListener);

        myProject.getMessageBus().connect(this).subscribe(RemoteRevisionChangeListener.class, this::scheduleRefresh);

        myProject.getMessageBus().connect(this).subscribe(CommitModeListener.class, () -> {
            myCommitPanelShown = false;
            rebuildContent();
        });

        DockLayout contentRoot = DockLayout.create();
        myContentRoot = contentRoot;
        rebuildContent();

        Content content = ContentFactory.getInstance().createUIContent(contentRoot, VcsLocalize.changesViewTabLocalChanges().get(), false);
        content.setCloseable(false);
        content.putUserData(ChangesViewContentI.CONTENT_ID_KEY, ChangesViewContentI.LOCAL_CHANGES);
        content.putUserData(ChangesViewContentI.IN_COMMIT_TOOL_WINDOW_KEY, Boolean.TRUE);
        myContentManager.addContent(content);
    }

    @RequiredUIAccess
    private void rebuildContent() {
        DockLayout contentRoot = myContentRoot;
        if (contentRoot == null || myDisposed) {
            return;
        }

        UnifiedCommitPanel oldPanel = myCommitPanel;
        if (oldPanel != null) {
            oldPanel.cancel();
        }

        Disposable contentDisposable = Disposable.newDisposable("Local Changes content");
        Disposer.register(this, contentDisposable);

        CommitMode commitMode = CommitModeManager.getInstance(myProject).getCurrentCommitMode();
        boolean panelMode = commitMode instanceof NonModalCommitMode nonModal && (!nonModal.toggleMode() || myCommitPanelShown);
        boolean inCommitToolWindow = panelMode && commitMode.isCommitToolWindowEnabled();

        UnifiedChangesTree oldTree = myTree;
        UnifiedChangesTree tree = new UnifiedChangesTree(myProject, panelMode, contentDisposable);
        if (oldTree != null) {
            tree.takeOver(oldTree);
        }
        myTree = tree;

        Disposable oldDisposable = myContentDisposable;
        if (oldDisposable != null) {
            Disposer.dispose(oldDisposable);
        }
        myContentDisposable = contentDisposable;

        Component treeComponent = createComponent(tree);
        if (panelMode) {
            UnifiedCommitPanel commitPanel = new UnifiedCommitPanel(myProject, tree, contentDisposable, this::refreshNow, this::commitPanelDeactivated);
            myCommitPanel = commitPanel;

            TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(
                inCommitToolWindow ? SplitLayoutPosition.VERTICAL : SplitLayoutPosition.HORIZONTAL
            );
            splitLayout.setProportion(inCommitToolWindow ? COMMIT_TOOL_WINDOW_TREE_PROPORTION : VCS_TOOL_WINDOW_TREE_PROPORTION);
            splitLayout.setFirstComponent(treeComponent);
            splitLayout.setSecondComponent(commitPanel.getComponent());
            contentRoot.center(splitLayout);

            commitPanel.activate();
        }
        else {
            myCommitPanel = null;
            contentRoot.center(treeComponent);
        }

        scheduleRefresh();
    }

    public boolean isCommitPanelActive() {
        return myCommitPanel != null;
    }

    @RequiredUIAccess
    public boolean activateCommitPanel(Collection<?> included, boolean force) {
        if (!(CommitModeManager.getInstance(myProject).getCurrentCommitMode() instanceof NonModalCommitMode)) {
            return false;
        }

        boolean wasActive = myCommitPanel != null;
        if (!wasActive) {
            myCommitPanelShown = true;
            rebuildContent();
        }

        UnifiedChangesTree tree = myTree;
        UnifiedCommitPanel commitPanel = myCommitPanel;
        if (tree == null || commitPanel == null) {
            return false;
        }

        if (!wasActive || force || (tree.getIncludedChanges().isEmpty() && tree.getIncludedUnversionedFiles().isEmpty())) {
            tree.setIncluded(included);
        }

        ToolWindow toolWindow = myContentManager.getToolWindowFor(ChangesViewContentI.LOCAL_CHANGES);
        if (toolWindow == null) {
            commitPanel.focusMessage();
            return true;
        }

        toolWindow.activate(() -> {
            myContentManager.selectContent(ChangesViewContentI.LOCAL_CHANGES);
            commitPanel.focusMessage();
        });
        return true;
    }

    @RequiredUIAccess
    public void deactivateCommitPanel() {
        if (!myCommitPanelShown) {
            return;
        }

        myCommitPanelShown = false;
        rebuildContent();
    }

    @RequiredUIAccess
    private void commitPanelDeactivated() {
        if (CommitModeManager.getInstance(myProject).getCurrentCommitMode() instanceof NonModalCommitMode nonModal && nonModal.toggleMode()) {
            deactivateCommitPanel();
        }
    }

    @RequiredUIAccess
    private Component createComponent(UnifiedChangesTree tree) {
        DockLayout root = DockLayout.create();
        root.putUserData(UiDataProvider.KEY, tree::uiDataSnapshot);

        ActionManager actionManager = ActionManager.getInstance();

        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        if (actionManager.getAction("ChangesViewToolbar") instanceof ActionGroup changesViewToolbar) {
            toolbarGroup.add(changesViewToolbar);
        }
        toolbarGroup.addSeparator();

        TreeExpander expander = TreeExpander.of(tree.getComponent());
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        toolbarGroup.add(commonActionsManager.createExpandAllAction(expander));
        toolbarGroup.add(commonActionsManager.createCollapseAllAction(expander));
        toolbarGroup.add(new ToggleCommitPanelAction());
        toolbarGroup.addSeparator();
        toolbarGroup.add(new ToggleShowDirectoriesAction());
        toolbarGroup.add(new ToggleShowIgnoredAction());

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, toolbarGroup, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(root);

        tree.getComponent().addContextMenuListener(event -> {
            if (!(actionManager.getAction("ChangesViewPopupMenu") instanceof ActionGroup group)) {
                return;
            }

            ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.CHANGES_VIEW_POPUP, group);
            menu.setTargetComponent(root);
            menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
        });

        Label statusLabel = Label.create(LocalizeValue.empty());
        statusLabel.setVisible(false);
        myStatusLabel = statusLabel;

        root.top(toolbar.getUIComponent());
        root.center(ScrollableLayout.create(tree.getComponent()));
        root.bottom(HorizontalLayout.create().add(statusLabel));
        return root;
    }

    @Override
    public void scheduleRefresh() {
        if (myDisposed || myApplication.isHeadlessEnvironment() || myProject.isDisposed()) {
            return;
        }

        synchronized (this) {
            ScheduledFuture<?> previous = myRefreshFuture;
            if (previous != null) {
                previous.cancel(false);
            }
            myRefreshFuture = AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(this::refreshInBackground, REFRESH_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void refreshInBackground() {
        if (myDisposed || myProject.isDisposed() || !myProject.isInitialized()) {
            return;
        }
        if (!ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) {
            return;
        }

        ChangesTreeSnapshot snapshot = buildSnapshot();
        UIAccess uiAccess = myProject.getUIAccess();
        if (!uiAccess.isValid()) {
            return;
        }
        uiAccess.give(() -> {
            UnifiedChangesTree tree = myTree;
            if (tree != null && !myDisposed) {
                tree.setSnapshot(snapshot);
            }
        });
    }

    @RequiredUIAccess
    public CompletableFuture<?> refreshNow() {
        UnifiedChangesTree tree = myTree;
        if (tree == null || myDisposed) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        return CompletableFuture.supplyAsync(this::buildSnapshot, AppExecutorUtil.getAppExecutorService())
            .thenComposeAsync(snapshot -> myTree == tree ? tree.setSnapshot(snapshot) : CompletableFuture.completedFuture(null), uiAccess);
    }

    private ChangesTreeSnapshot buildSnapshot() {
        ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(myProject);

        ChangesTreeSnapshotBuilder builder = new ChangesTreeSnapshotBuilder(myProject, myState.myShowFlatten)
            .changeLists(changeListManager.getChangeListsCopy())
            .files(ChangesTreeGroupKind.MODIFIED_WITHOUT_EDITING, changeListManager.getModifiedWithoutEditing())
            .locallyDeleted(changeListManager.getDeletedFiles())
            .files(ChangesTreeGroupKind.UNVERSIONED, changeListManager.getUnversionedFiles());

        if (myState.myShowIgnored) {
            builder.files(ChangesTreeGroupKind.IGNORED, changeListManager.getIgnoredFiles());
        }
        return builder.build();
    }

    @Override
    public void selectFile(@Nullable VirtualFile vFile) {
        UnifiedChangesTree tree = myTree;
        UIAccess uiAccess = myProject.getUIAccess();
        if (vFile == null || tree == null || !uiAccess.isValid()) {
            return;
        }

        uiAccess.give(() -> {
            List<ChangesTreeItem> path = findPath(tree.getSnapshot(), null, vFile, new ArrayList<>());
            if (path != null) {
                tree.selectPath(path);
            }
        });
    }

    private static @Nullable List<ChangesTreeItem> findPath(ChangesTreeSnapshot snapshot,
                                                           @Nullable ChangesTreeItem parent,
                                                           VirtualFile file,
                                                           List<ChangesTreeItem> prefix) {
        for (ChangesTreeItem child : snapshot.getChildren(parent)) {
            prefix.add(child);
            if (matches(child, file)) {
                return List.copyOf(prefix);
            }
            if (snapshot.isGroup(child)) {
                List<ChangesTreeItem> found = findPath(snapshot, child, file, prefix);
                if (found != null) {
                    return found;
                }
            }
            prefix.removeLast();
        }
        return null;
    }

    private static boolean matches(ChangesTreeItem item, VirtualFile file) {
        if (item instanceof ChangeTreeItem changeItem) {
            return file.equals(ChangesUtil.getFilePath(changeItem.change()).getVirtualFile());
        }
        return item instanceof FileTreeItem fileItem && file.equals(fileItem.file());
    }

    @Override
    public void refreshChangesViewNodeAsync(VirtualFile file) {
        scheduleRefresh();
    }

    @Override
    public void updateProgressText(String text, boolean isError) {
        UIAccess uiAccess = myProject.getUIAccess();
        if (!uiAccess.isValid()) {
            return;
        }

        uiAccess.give(() -> {
            Label label = myStatusLabel;
            if (label != null) {
                label.setText(LocalizeValue.of(text));
                label.setForegroundColor(isError ? TextAttribute.ERROR.getForegroundColor() : null);
                label.setVisible(!text.isEmpty());
            }
        });
    }

    @Override
    public void setBusy(boolean b) {
    }

    @Override
    public void setShowFlattenMode(boolean state) {
        myState.myShowFlatten = state;
        scheduleRefresh();
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    @Override
    public void dispose() {
        myDisposed = true;

        ChangeListManager.getInstance(myProject).removeChangeListListener(myListener);

        synchronized (this) {
            ScheduledFuture<?> future = myRefreshFuture;
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    private class ToggleCommitPanelAction extends DumbAwareToggleAction {
        private ToggleCommitPanelAction() {
            super(
                VcsLocalize.changesActionToggleCommitPanelText(),
                VcsLocalize.changesActionToggleCommitPanelDescription(),
                PlatformIconGroup.actionsCommit()
            );
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            boolean toggleMode = CommitModeManager.getInstance(myProject).getCurrentCommitMode() instanceof NonModalCommitMode nonModal
                && nonModal.toggleMode();
            e.getPresentation().setEnabledAndVisible(toggleMode);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myCommitPanel != null;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                activateCommitPanel(ChangeListManager.getInstance(myProject).getDefaultChangeList().getChanges(), false);
            }
            else {
                deactivateCommitPanel();
            }
        }
    }

    private class ToggleShowDirectoriesAction extends DumbAwareToggleAction {
        private ToggleShowDirectoriesAction() {
            super(
                VcsLocalize.changesActionShowDirectoriesText(),
                VcsLocalize.changesActionShowDirectoriesDescription(),
                AllIcons.Actions.GroupByPackage
            );
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return !myState.myShowFlatten;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            setShowFlattenMode(!state);
        }
    }

    private class ToggleShowIgnoredAction extends DumbAwareToggleAction {
        private ToggleShowIgnoredAction() {
            super(
                VcsLocalize.changesActionShowIgnoredText(),
                VcsLocalize.changesActionShowIgnoredDescription(),
                PlatformIconGroup.actionsTogglevisibility()
            );
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myState.myShowIgnored;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myState.myShowIgnored = state;
            scheduleRefresh();
        }
    }
}
