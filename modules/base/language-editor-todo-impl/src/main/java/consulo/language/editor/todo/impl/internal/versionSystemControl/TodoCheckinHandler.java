/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.todo.impl.internal.versionSystemControl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.ui.util.TodoPanelSettings;
import consulo.application.util.DateFormatUtil;
import consulo.language.editor.todo.TodoConfiguration;
import consulo.language.editor.todo.TodoFilter;
import consulo.language.editor.todo.impl.internal.CustomChangelistTodosTreeBuilder;
import consulo.language.editor.todo.impl.internal.TodoView;
import consulo.language.editor.todo.impl.internal.action.SetTodoFilterAction;
import consulo.language.editor.todo.impl.internal.localize.LanguageTodoLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static consulo.application.CommonBundle.getCancelButtonText;
import static consulo.ui.ex.awt.Messages.*;
import static consulo.ui.ex.awt.UIUtil.getWarningIcon;

/**
 * @author irengrig
 * @since 2011-02-17
 */
public class TodoCheckinHandler extends CheckinHandler {
    private final Project myProject;
    private final CheckinProjectPanel myCheckinProjectPanel;
    private final VcsConfiguration myConfiguration;
    private TodoFilter myTodoFilter;

    public TodoCheckinHandler(CheckinProjectPanel checkinProjectPanel) {
        myProject = checkinProjectPanel.getProject();
        myCheckinProjectPanel = checkinProjectPanel;
        myConfiguration = VcsConfiguration.getInstance(myProject);
    }

    @RequiredUIAccess
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        CheckBox checkBox = CheckBox.create(VcsLocalize.beforeCheckinNewTodoCheck(""));
        return new RefreshableOnComponent() {
            @RequiredUIAccess
            @Nonnull
            @Override
            public Component getUIComponent() {
                HorizontalLayout panel = HorizontalLayout.create();
                panel.add(checkBox);

                setFilterText(myConfiguration.myTodoPanelSettings.todoFilterName);
                if (myConfiguration.myTodoPanelSettings.todoFilterName != null) {
                    myTodoFilter = TodoConfiguration.getInstance().getTodoFilter(myConfiguration.myTodoPanelSettings.todoFilterName);
                }

                @RequiredUIAccess Consumer<TodoFilter> consumer = todoFilter -> {
                    myTodoFilter = todoFilter;
                    String name = todoFilter == null ? null : todoFilter.getName();
                    myConfiguration.myTodoPanelSettings.todoFilterName = name;
                    setFilterText(name);
                };

                Button configureButton = Button.create(LocalizeValue.empty());
                configureButton.setToolTipText(LocalizeValue.localizeTODO("Configure"));
                configureButton.setIcon(PlatformIconGroup.generalGearplain());
                configureButton.addStyle(ButtonStyle.TOOLBAR);

                configureButton.addClickListener(event -> {
                    SetTodoFilterAction group = new SetTodoFilterAction(myProject, myConfiguration.myTodoPanelSettings, consumer);
                    ActionPopupMenu popupMenu =
                            ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TODO_VIEW_TOOLBAR, group);
                    popupMenu.showUnderneathOf(configureButton);
                });

                panel.add(configureButton);

                CheckinHandlerUtil.disableWhenDumb(
                        myProject,
                        checkBox,
                        LocalizeValue.localizeTODO("TODO check is impossible until indices are up-to-date")
                );
                return panel;
            }

            @RequiredUIAccess
            private void setFilterText(String filterName) {
                if (filterName == null) {
                    checkBox.setLabelText(VcsLocalize.beforeCheckinNewTodoCheck(LanguageTodoLocalize.actionTodoShowAll()));
                }
                else {
                    checkBox.setLabelText(VcsLocalize.beforeCheckinNewTodoCheck("Filter: " + filterName));
                }
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                myConfiguration.CHECK_NEW_TODO = checkBox.getValueOrError();
            }

            @Override
            @RequiredUIAccess
            public void restoreState() {
                checkBox.setValue(myConfiguration.CHECK_NEW_TODO);
            }
        };
    }

    @Override
    @RequiredUIAccess
    public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, BiConsumer<Object, Object> additionalDataConsumer) {
        if (!myConfiguration.CHECK_NEW_TODO) {
            return ReturnResult.COMMIT;
        }
        if (DumbService.getInstance(myProject).isDumb()) {
            LocalizeValue todoName = VcsLocalize.beforeCheckinNewTodoCheckTitle();
            if (Messages.showOkCancelDialog(
                    myProject,
                    todoName +
                            " can't be performed while " + Application.get().getName().get() + " updates the indices in background.\n" +
                            "You can commit the changes without running checks, or you can wait until indices are built.",
                    todoName + " is not possible right now",
                    "&Wait",
                    "&Commit",
                    null
            ) == Messages.OK) {
                return ReturnResult.CANCEL;
            }
            return ReturnResult.COMMIT;
        }
        Collection<Change> changes = myCheckinProjectPanel.getSelectedChanges();
        TodoCheckinHandlerWorker worker = new TodoCheckinHandlerWorker(myProject, changes, myTodoFilter, true);

        SimpleReference<Boolean> completed = SimpleReference.create(Boolean.FALSE);
        ProgressManager.getInstance().run(
                new Task.Modal(myProject, LocalizeValue.localizeTODO("Looking for New and Edited TODO Items..."), true) {
                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        worker.execute();
                    }

                    @Override
                    @RequiredUIAccess
                    public void onSuccess() {
                        completed.set(Boolean.TRUE);
                    }
                }
        );
        if (completed.get() && (worker.getAddedOrEditedTodos().isEmpty() && worker.getInChangedTodos().isEmpty() &&
                worker.getSkipped().isEmpty())) {
            return ReturnResult.COMMIT;
        }
        if (!completed.get()) {
            return ReturnResult.CANCEL;
        }
        return showResults(worker, executor);
    }

    @RequiredUIAccess
    private ReturnResult showResults(TodoCheckinHandlerWorker worker, CommitExecutor executor) {
        LocalizeValue commitButtonText = executor != null ? executor.getActionText() : myCheckinProjectPanel.getCommitActionName();
        commitButtonText = commitButtonText.map(text -> StringUtil.trimEnd(text, "..."));

        LocalizeValue text = createMessage(worker);
        boolean thereAreTodoFound = worker.getAddedOrEditedTodos().size() + worker.getInChangedTodos().size() > 0;
        String title = "TODO";
        if (thereAreTodoFound) {
            return askReviewOrCommit(worker, commitButtonText, text, title);
        }
        else if (YES == showYesNoDialog(
                myProject,
                text.get(),
                title,
                commitButtonText.get(),
                CommonLocalize.buttonCancel().get(),
                getWarningIcon()
        )) {
            return ReturnResult.COMMIT;
        }
        return ReturnResult.CANCEL;
    }

    @Nonnull
    @RequiredUIAccess
    private ReturnResult askReviewOrCommit(
            @Nonnull TodoCheckinHandlerWorker worker,
            @Nonnull LocalizeValue commitButton,
            @Nonnull LocalizeValue text,
            @Nonnull String title
    ) {
        LocalizeValue yesButton = VcsLocalize.todoInNewReviewButton();
        int result = showYesNoCancelDialog(
                myProject,
                text.get(),
                title,
                yesButton.get(),
                commitButton.get(),
                getCancelButtonText(),
                getWarningIcon()
        );
        return switch (result) {
            case YES -> {
                showTodo(worker);
                yield ReturnResult.CLOSE_WINDOW;
            }
            case NO -> ReturnResult.COMMIT;
            default -> ReturnResult.CANCEL;
        };
    }

    private void showTodo(TodoCheckinHandlerWorker worker) {
        String title = "For commit (" + DateFormatUtil.formatDateTime(System.currentTimeMillis()) + ")";
        myProject.getInstance(TodoView.class).addCustomTodoView(
                (tree, project) -> new CustomChangelistTodosTreeBuilder(tree, myProject, title, worker.inOneList()),
                title,
                new TodoPanelSettings(myConfiguration.myTodoPanelSettings)
        );

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager manager = ToolWindowManager.getInstance(myProject);
            if (manager != null) {
                ToolWindow window = manager.getToolWindow("TODO");
                if (window != null) {
                    window.show(() -> {
                        ContentManager cm = window.getContentManager();
                        Content[] contents = cm.getContents();
                        if (contents.length > 0) {
                            cm.setSelectedContent(contents[contents.length - 1], true);
                        }
                    });
                }
            }
        }, ModalityState.nonModal(), myProject.getDisposed());
    }

    private static LocalizeValue createMessage(TodoCheckinHandlerWorker worker) {
        int added = worker.getAddedOrEditedTodos().size();
        int changed = worker.getInChangedTodos().size();
        int skipped = worker.getSkipped().size();
        if (added == 0 && changed == 0) {
            return VcsLocalize.todoHandlerOnlySkipped(skipped);
        }
        else if (changed == 0) {
            return VcsLocalize.todoHandlerOnlyAdded(added, skipped);
        }
        else if (added == 0) {
            return VcsLocalize.todoHandlerOnlyInChanged(changed, skipped);
        }
        else {
            return VcsLocalize.todoHandlerOnlyBoth(added, changed, skipped);
        }
    }
}
