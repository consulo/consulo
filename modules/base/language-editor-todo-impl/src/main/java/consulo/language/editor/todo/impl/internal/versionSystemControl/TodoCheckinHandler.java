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

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
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
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.HorizontalLayout;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerUtil;
import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.checkin.CommitInfo;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author irengrig
 * @since 2011-02-17
 */
public class TodoCheckinHandler extends CheckinHandler implements CommitCheck, DumbAware {
    private final Project myProject;
    private final VcsConfiguration myConfiguration;
    private @Nullable TodoFilter myTodoFilter;

    public TodoCheckinHandler(Project project) {
        myProject = project;
        myConfiguration = VcsConfiguration.getInstance(project);
    }

    @Override
    public ExecutionOrder getExecutionOrder() {
        return ExecutionOrder.POST_COMMIT;
    }

    @RequiredUIAccess
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        CheckBox checkBox = CheckBox.create(VcsLocalize.beforeCheckinNewTodoCheck(""));
        return new RefreshableOnComponent() {
            @RequiredUIAccess
            
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
    public boolean isEnabled() {
        return myConfiguration.CHECK_NEW_TODO;
    }

    @Override
    public @Nullable TodoCommitProblem runCheck(CommitInfo commitInfo, ProgressIndicator indicator) {
        String filterName = myConfiguration.myTodoPanelSettings.todoFilterName;
        TodoFilter todoFilter = filterName == null ? null : TodoConfiguration.getInstance().getTodoFilter(filterName);
        TodoCheckinHandlerWorker worker = new TodoCheckinHandlerWorker(myProject, commitInfo.getCommittedChanges(), todoFilter, true);

        indicator.setIndeterminate(true);
        indicator.setText(VcsLocalize.progressTextCheckingForTodo());
        worker.execute();

        boolean noTodo = worker.inOneList().isEmpty();
        boolean noSkipped = worker.getSkipped().isEmpty();
        if (noTodo && noSkipped) {
            return null;
        }
        return new TodoCommitProblem(worker);
    }

    @RequiredUIAccess
    static CompletableFuture<ReturnResult> showDialog(Project project, TodoCheckinHandlerWorker worker, LocalizeValue commitActionText) {
        boolean noTodo = worker.getAddedOrEditedTodos().isEmpty() && worker.getInChangedTodos().isEmpty();
        boolean noSkipped = worker.getSkipped().isEmpty();

        if (noTodo && noSkipped) {
            return CompletableFuture.completedFuture(ReturnResult.COMMIT);
        }

        if (noTodo) {
            return MessageBoxBuilder.<ReturnResult>create()
                .asWarning()
                .richText()
                .title(VcsLocalize.checkinDialogTitleTodo())
                .text(getDescription(worker))
                .button(MessageButtonRole.YES, commitActionText, ReturnResult.COMMIT)
                .asDefaultButton()
                .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), ReturnResult.CANCEL)
                .asExitButton()
                .showAsync();
        }

        return MessageBoxBuilder.<ReturnResult>create()
            .asWarning()
            .richText()
            .title(VcsLocalize.checkinDialogTitleTodo())
            .text(getDescription(worker))
            .button(MessageButtonRole.YES, VcsLocalize.todoInNewReviewButton(), () -> {
                showTodoItems(project, worker);
                return ReturnResult.CLOSE_WINDOW;
            })
            .asDefaultButton()
            .button(MessageButtonRole.NO, commitActionText, ReturnResult.COMMIT)
            .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), ReturnResult.CANCEL)
            .asExitButton()
            .showAsync();
    }

    @RequiredUIAccess
    static void showTodoItems(Project project, TodoCheckinHandlerWorker worker) {
        String title = VcsLocalize.checkinTitleForCommit0(DateFormatUtil.formatDateTime(System.currentTimeMillis())).get();
        project.getInstance(TodoView.class).addCustomTodoView(
            (tree, p) -> new CustomChangelistTodosTreeBuilder(tree, project, title, worker.inOneList()),
            title,
            new TodoPanelSettings(VcsConfiguration.getInstance(project).myTodoPanelSettings)
        );

        UIAccess uiAccess = project.getUIAccess();
        if (!uiAccess.isValid()) {
            return;
        }

        uiAccess.giveLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TODO");
            if (toolWindow != null) {
                toolWindow.show(() -> {
                    ContentManager contentManager = toolWindow.getContentManager();
                    Content[] contents = contentManager.getContents();
                    if (contents.length > 0) {
                        contentManager.setSelectedContent(contents[contents.length - 1], true);
                    }
                });
            }
        }, ModalityState.nonModal());
    }

    private static LocalizeValue getDescription(TodoCheckinHandlerWorker worker) {
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
