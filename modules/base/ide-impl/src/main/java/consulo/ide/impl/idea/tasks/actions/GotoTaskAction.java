package consulo.ide.impl.idea.tasks.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameBase;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameItemProvider;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopup;
import consulo.ide.impl.idea.ide.util.gotoByName.SimpleChooseByNameModel;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.tasks.doc.TaskPsiElement;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.impl.internal.action.ConfigureServersAction;
import consulo.task.impl.internal.action.TaskSearchSupport;
import consulo.task.localize.TaskLocalize;
import consulo.task.util.TaskUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Evgeny Zakrevsky
 */
@ActionImpl(id = GotoTaskAction.ID)
public class GotoTaskAction extends GotoActionBase implements DumbAware {
    public static final String ID = "tasks.goto";
    public static final CreateNewTaskAction CREATE_NEW_TASK_ACTION = new CreateNewTaskAction();

    @Nonnull
    private final Application myApplication;

    @Inject
    public GotoTaskAction(@Nonnull Application application) {
        super(TaskLocalize.openTaskActionMenuText(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    protected void gotoActionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            perform(project);
        }
    }

    @RequiredUIAccess
    void perform(final Project project) {
        final SimpleReference<Boolean> shiftPressed = SimpleReference.create(false);

        final ChooseByNamePopup popup = ChooseByNamePopup.createPopup(
            project,
            new GotoTaskPopupModel(project),
            new ChooseByNameItemProvider() {
                @Nonnull
                @Override
                public List<String> filterNames(@Nonnull ChooseByNameBase base, @Nonnull String[] names, @Nonnull String pattern) {
                    return Collections.emptyList();
                }

                @Override
                public boolean filterElements(
                    @Nonnull ChooseByNameBase base,
                    @Nonnull String pattern,
                    boolean everywhere,
                    @Nonnull ProgressIndicator cancelled,
                    @Nonnull Predicate<Object> consumer
                ) {
                    CREATE_NEW_TASK_ACTION.setTaskName(pattern);
                    if (!consumer.test(CREATE_NEW_TASK_ACTION)) {
                        return false;
                    }

                    List<Task> cachedAndLocalTasks =
                        TaskSearchSupport.getLocalAndCachedTasks(TaskManager.getManager(project), pattern, everywhere);
                    boolean cachedTasksFound = !cachedAndLocalTasks.isEmpty();
                    if (!processTasks(cachedAndLocalTasks, consumer, cancelled, PsiManager.getInstance(project))) {
                        return false;
                    }

                    List<Task> tasks = TaskSearchSupport.getRepositoriesTasks(
                        TaskManager.getManager(project),
                        pattern,
                        base.getMaximumListSizeLimit(),
                        0,
                        true,
                        everywhere,
                        cancelled
                    );
                    tasks.removeAll(cachedAndLocalTasks);

                    return processTasks(tasks, consumer, cancelled, PsiManager.getInstance(project));
                }
            },
            null,
            false,
            0
        );

        popup.setShowListForEmptyPattern(true);
        popup.setSearchInAnyPlace(true);
        popup.setAdText(
            TaskLocalize.popupAdvertisementHtmlPressShiftToMergeWithCurrentContextBrPressingWouldShowTaskDescriptionCommentsHtml(
                KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC))
            ).get()
        );
        popup.registerAction(
            "shiftPressed",
            KeyStroke.getKeyStroke("shift pressed SHIFT"),
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    shiftPressed.set(true);
                }
            }
        );
        popup.registerAction(
            "shiftReleased",
            KeyStroke.getKeyStroke("released SHIFT"),
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    shiftPressed.set(false);
                }
            }
        );

        DefaultActionGroup group = new DefaultActionGroup(new ConfigureServersAction(myApplication) {
            @Override
            protected void serversChanged() {
                popup.rebuildList(true);
            }
        });
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        actionToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        actionToolbar.updateActionsAsync();
        actionToolbar.getComponent().setFocusable(false);
        actionToolbar.getComponent().setBorder(null);
        popup.setToolArea(actionToolbar.getComponent());
        popup.setMaximumListSizeLimit(10);
        popup.setListSizeIncreasing(10);

        showNavigationPopup(
            new GotoActionCallback<>() {
                @Override
                public void elementChosen(ChooseByNamePopup popup, Object element) {
                    TaskManager taskManager = TaskManager.getManager(project);
                    if (element instanceof TaskPsiElement taskPsiElem) {
                        Task task = taskPsiElem.getTask();
                        LocalTask localTask = taskManager.findTask(task.getId());
                        if (localTask != null) {
                            taskManager.activateTask(localTask, !shiftPressed.get());
                        }
                        else {
                            showOpenTaskDialog(project, task);
                        }
                    }
                    else if (element == CREATE_NEW_TASK_ACTION) {
                        LocalTask localTask = taskManager.createLocalTask(CREATE_NEW_TASK_ACTION.getTaskName());
                        showOpenTaskDialog(project, localTask);
                    }
                }
            },
            null,
            popup
        );
    }

    private static boolean processTasks(List<Task> tasks, Predicate<Object> consumer, ProgressIndicator cancelled, PsiManager psiManager) {
        for (Task task : tasks) {
            cancelled.checkCanceled();
            if (!consumer.test(new TaskPsiElement(psiManager, task))) {
                return false;
            }
        }
        return true;
    }

    private static void showOpenTaskDialog(@Nonnull Project project, Task task) {
        JBPopup hint = DocumentationManager.getInstance(project).getDocInfoHint();
        if (hint != null) {
            hint.cancel();
        }
        project.getApplication().invokeLater(() -> new OpenTaskDialog(project, task).show());
    }

    private static class GotoTaskPopupModel extends SimpleChooseByNameModel {
        private ListCellRenderer myListCellRenderer;

        protected GotoTaskPopupModel(@Nonnull Project project) {
            super(project, TaskLocalize.enterTaskName().get(), null);
            myListCellRenderer = new TaskCellRenderer(project);
        }

        @Override
        public String[] getNames() {
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }

        @Override
        protected Object[] getElementsByName(String name, String pattern) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public ListCellRenderer getListCellRenderer() {
            return myListCellRenderer;
        }

        @Override
        public String getElementName(Object element) {
            if (element instanceof TaskPsiElement taskPsiElem) {
                return TaskUtil.getTrimmedSummary(taskPsiElem.getTask());
            }
            else if (element == CREATE_NEW_TASK_ACTION) {
                return CREATE_NEW_TASK_ACTION.getActionText().get();
            }
            return null;
        }

        @Nonnull
        @Override
        public LocalizeValue getCheckBoxName() {
            return TaskLocalize.labelIncludeClosedTasks();
        }

        @Override
        public void saveInitialCheckBoxState(boolean state) {
            ((TaskManagerImpl) TaskManager.getManager(getProject())).getState().searchClosedTasks = state;
        }

        @Override
        public boolean loadInitialCheckBoxState() {
            return ((TaskManagerImpl) TaskManager.getManager(getProject())).getState().searchClosedTasks;
        }
    }

    public static class CreateNewTaskAction {
        private String taskName;

        public LocalizeValue getActionText() {
            return TaskLocalize.createNewTask0(taskName);
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getTaskName() {
            return taskName;
        }
    }
}
