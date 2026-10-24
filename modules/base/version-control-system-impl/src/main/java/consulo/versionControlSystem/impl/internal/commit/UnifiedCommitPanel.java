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
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.PopupPosition;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.impl.internal.change.ui.UnifiedChangesTree;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitPanel implements UnifiedCommitWorkflowUi {
    private static final Logger LOG = Logger.getInstance(UnifiedCommitPanel.class);

    private static final String MESSAGE_ACTION_GROUP = "Vcs.MessageActionGroup";

    private final Project myProject;
    private final UnifiedChangesTree myTree;

    private final DockLayout myRoot;
    private final EditorBox myMessageBox;
    private final OptionButton myCommitButton;
    private final HorizontalLayout myButtons;
    private final List<Button> myExecutorButtons = new ArrayList<>();
    private final UnifiedCommitStatus myStatus;
    private final Disposable myParentDisposable;
    private final Runnable myDeactivateListener;
    private final Supplier<CompletableFuture<?>> myRefreshChanges;

    private @Nullable UnifiedCommitWorkflowHandler myHandler;
    private @Nullable UnifiedCommitOptions myCommitOptions;
    private @Nullable Disposable myCommitOptionsDisposable;
    private LocalizeValue myCommitActionName = VcsLocalize.commitDialogDefaultCommitOperationName();

    @RequiredUIAccess
    public UnifiedCommitPanel(Project project,
                              UnifiedChangesTree tree,
                              Disposable parent,
                              Supplier<CompletableFuture<?>> refreshChanges,
                              Runnable deactivateListener) {
        myProject = project;
        myTree = tree;
        myRefreshChanges = refreshChanges;
        myDeactivateListener = deactivateListener;

        myMessageBox = project.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .multiline()
            .placeholder(VcsLocalize.commitPanelMessagePlaceholder())
            .build();

        myCommitButton = OptionButton.create(VcsLocalize.commitDialogDefaultCommitOperationName(), event -> commit(null));
        myCommitButton.addStyle(ButtonStyle.PRIMARY);
        myCommitButton.setEnabled(false);

        myButtons = HorizontalLayout.create();

        myStatus = new UnifiedCommitStatus(project);

        myParentDisposable = parent;

        myRoot = DockLayout.create();
        myRoot.putUserData(UiDataProvider.KEY, sink -> {
            UnifiedCommitWorkflowHandler handler = myHandler;
            if (handler != null) {
                sink.set(Refreshable.PANEL_KEY, handler.getWorkflow().getProjectPanel());
            }
        });

        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        if (ActionManager.getInstance().getAction(MESSAGE_ACTION_GROUP) instanceof ActionGroup messageGroup) {
            toolbarGroup.add(messageGroup);
        }
        toolbarGroup.add(new ShowCommitOptionsAction());
        ActionToolbar optionsToolbar = createToolbar(toolbarGroup);

        DockLayout buttonRow = DockLayout.create();
        buttonRow.left(myButtons);
        buttonRow.right(optionsToolbar.getUIComponent());

        VerticalLayout bottom = VerticalLayout.create();
        bottom.add(myStatus.getComponent());
        bottom.add(buttonRow);

        myRoot.center(myMessageBox);
        myRoot.bottom(bottom);
        myRoot.paddingBuilder().allSet(Space.SMALL).apply();

        tree.addInclusionListener(this::updateCommitButton);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(ActionGroup group) {
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(myRoot);
        return toolbar;
    }

    @RequiredUIAccess
    public void activate() {
        LocalizeValue actionName = commitActionName();
        myCommitActionName = actionName;
        myCommitButton.setText(actionName);

        UnifiedCommitWorkflow workflow = new UnifiedCommitWorkflow(
            myProject,
            myProject.getApplication().getInstance(ProgressBuilderFactory.class),
            List.of(),
            false,
            null,
            null
        );
        UnifiedCommitWorkflowHandler handler = new UnifiedCommitWorkflowHandler(workflow, this, actionName);
        resetCommitOptions();
        myHandler = handler;
        handler.activate();

        updateExecutors();
        updateCommitButton();
    }

    private LocalizeValue commitActionName() {
        AbstractVcs[] vcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
        if (vcses.length == 1) {
            CheckinEnvironment environment = vcses[0].getCheckinEnvironment();
            if (environment != null) {
                return environment.getCheckinOperationName();
            }
        }
        return VcsLocalize.commitDialogDefaultCommitOperationName();
    }

    @RequiredUIAccess
    private void updateExecutors() {
        myButtons.removeAll();
        myExecutorButtons.clear();
        myButtons.add(myCommitButton);

        for (AbstractVcs<?> vcs : ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss()) {
            for (CommitExecutor executor : vcs.getCommitExecutors()) {
                Button button = Button.create(executor.getActionText(), event -> commit(executor));
                myExecutorButtons.add(button);
                myButtons.add(button);
            }
        }

        List<OptionButtonItem> options = new ArrayList<>();
        for (CommitExecutor executor : ChangeListManager.getInstance(myProject).getRegisteredExecutors()) {
            options.add(OptionButtonItem.of(executor.getActionText(), event -> commit(executor)));
        }
        myCommitButton.setOptions(options);
    }

    @RequiredUIAccess
    private void updateCommitButton() {
        UnifiedCommitWorkflowHandler handler = myHandler;
        boolean executing = handler != null && handler.getWorkflow().isExecuting();
        boolean enabled = !executing && (!getIncludedChanges().isEmpty() || !getIncludedUnversionedFiles().isEmpty());
        myCommitButton.setEnabled(enabled);
        for (Button button : myExecutorButtons) {
            button.setEnabled(enabled);
        }
    }

    @RequiredUIAccess
    private void commit(@Nullable CommitExecutor executor) {
        UnifiedCommitWorkflowHandler handler = myHandler;
        if (handler == null) {
            return;
        }

        UnifiedCommitOptions options = myCommitOptions;
        if (options != null) {
            options.saveState();
        }

        myCommitButton.setEnabled(false);
        for (Button button : myExecutorButtons) {
            button.setEnabled(false);
        }
        UIAccess uiAccess = UIAccess.current();
        handler.executorCalled(executor).whenCompleteAsync((committed, error) -> {
            if (error != null) {
                LOG.error(error);
            }
            updateCommitButton();
        }, uiAccess);
    }

    @RequiredUIAccess
    private @Nullable UnifiedCommitOptions ensureCommitOptions() {
        UnifiedCommitWorkflowHandler handler = myHandler;
        if (handler == null) {
            return null;
        }

        UnifiedCommitOptions options = myCommitOptions;
        if (options == null) {
            Disposable optionsDisposable = Disposable.newDisposable("Commit options");
            Disposer.register(myParentDisposable, optionsDisposable);
            myCommitOptionsDisposable = optionsDisposable;

            options = new UnifiedCommitOptions();
            options.fill(handler.getWorkflow(), myCommitActionName, true, optionsDisposable);
            options.getComponent().paddingBuilder().allSet(Space.MEDIUM).apply();
            myCommitOptions = options;
        }
        return options;
    }

    @RequiredUIAccess
    private void resetCommitOptions() {
        myCommitOptions = null;
        Disposable optionsDisposable = myCommitOptionsDisposable;
        myCommitOptionsDisposable = null;
        if (optionsDisposable != null) {
            Disposer.dispose(optionsDisposable);
        }
    }

    @RequiredUIAccess
    public void focusMessage() {
        myMessageBox.focus();
    }

    @RequiredUIAccess
    public void cancel() {
        UnifiedCommitWorkflowHandler handler = myHandler;
        if (handler != null) {
            handler.cancelled();
        }
    }

    @Override
    public LocalChangeList getChangeList() {
        return ChangeListManager.getInstance(myProject).getDefaultChangeList();
    }

    @Override
    public List<Change> getDisplayedChanges() {
        return List.copyOf(ChangeListManager.getInstance(myProject).getAllChanges());
    }

    @Override
    public List<Change> getIncludedChanges() {
        return myTree.getIncludedChanges();
    }

    @Override
    public List<VirtualFile> getIncludedUnversionedFiles() {
        return myTree.getIncludedUnversionedFiles();
    }

    @RequiredUIAccess
    @Override
    public void includeChanges(Collection<Change> changes) {
        myTree.includeChanges(changes);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> refreshChangesBeforeCommit() {
        return myRefreshChanges.get();
    }

    @Override
    public String getCommitMessage() {
        String value = myMessageBox.getValue();
        return value == null ? "" : value;
    }

    @RequiredUIAccess
    @Override
    public void setCommitMessage(String commitMessage) {
        myMessageBox.setValue(commitMessage);
    }

    @RequiredUIAccess
    @Override
    public void startBeforeCommitChecks() {
        myStatus.showRunning();
    }

    @RequiredUIAccess
    @Override
    public void endBeforeCommitChecks(CheckinHandler.ReturnResult result, @Nullable CommitProblem problem) {
        if (problem != null) {
            myStatus.showProblem(VcsLocalize.commitPanelChecksFailed(problem.getText()), problem);
        }
        else {
            myStatus.hide();
        }
        updateCommitButton();
    }

    @RequiredUIAccess
    @Override
    public void deactivate(boolean committed) {
        myStatus.hide();
        if (committed) {
            setCommitMessage("");
            resetCommitOptions();
        }
        updateCommitButton();
        myDeactivateListener.run();
    }

    private class ShowCommitOptionsAction extends DumbAwareAction {
        private ShowCommitOptionsAction() {
            super(VcsLocalize.commitPanelOptions(), LocalizeValue.empty(), PlatformIconGroup.generalGearplain());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            UnifiedCommitOptions options = ensureCommitOptions();
            if (options == null || options.isEmpty()) {
                return;
            }

            JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(options.getComponent(), null)
                .setRequestFocus(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void beforeShown(LightweightWindowEvent event) {
                        options.restoreState();
                    }

                    @Override
                    public void onClosed(LightweightWindowEvent event) {
                        options.saveState();
                    }
                })
                .createPopup();

            ComponentEvent<?> uiEvent = e.getUIEvent();
            if (uiEvent != null) {
                popup.showBy(uiEvent.getComponent(), null, PopupPosition.TOP);
            }
            else {
                popup.showUnderneathOf(e);
            }
        }
    }
}
