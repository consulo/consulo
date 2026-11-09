/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.task.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.event.ListPopupKeyListener;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "tasks.switch")
public class SwitchTaskAction extends BaseTaskAction {
    private static final Logger LOG = Logger.getInstance(SwitchTaskAction.class);

    public SwitchTaskAction() {
      super(TaskLocalize.actionTasksSwitchText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ListPopup popup = createPopup(e.getDataContext(), null, true);
        popup.showCenteredInCurrentWindow(project);
    }

    public static ListPopup createPopup(DataContext dataContext, @Nullable Runnable onDispose, boolean withTitle) {
        final Project project = dataContext.getData(Project.KEY);
        final SimpleReference<Boolean> shiftPressed = SimpleReference.create(false);
        List<TaskListItem> items = project == null ? Collections.<TaskListItem>emptyList() :
            createPopupActionGroup(project, shiftPressed, dataContext);
        final String title = withTitle ? TaskLocalize.popupTitleSwitchToTask().get() : null;
        ListPopupStep<TaskListItem> step = new MultiSelectionListPopupStep<>(title, items) {
            @Override
            public PopupStep<?> onChosen(List<TaskListItem> selectedValues, boolean finalChoice) {
                if (finalChoice) {
                    return doFinalStep(() -> selectedValues.get(0).select());
                }
                ActionGroup group = createActionsStep(selectedValues, project, shiftPressed);
                return JBPopupFactory.getInstance().createActionsStep(
                    group,
                    dataContext,
                    null,
                    false,
                    false,
                    null,
                    null,
                    true,
                    0,
                    false
                );
            }

            @Override
            public Image getIconFor(TaskListItem aValue) {
                return aValue.getIcon();
            }

            
            @Override
            public String getTextFor(TaskListItem value) {
                return value.getText();
            }

            @Override
            public @Nullable ListSeparator getSeparatorAbove(TaskListItem value) {
                return value.getSeparator() == null ? null : new ListSeparator(value.getSeparator());
            }

            @Override
            public boolean hasSubstep(List<TaskListItem> selectedValues) {
                return selectedValues.size() > 1 || selectedValues.get(0).getTask() != null;
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        if (items.size() <= 2) {
            return popup;
        }

        popup.setAdText(TaskLocalize.popupAdvertisementPressShiftToMergeWithCurrentContext().get());
        popup.addKeyListener(new ListPopupKeyListener() {
            @Override
            @RequiredUIAccess
            public boolean keyPressed(ListPopup listPopup, KeyboardInputDetails details) {
                if (KeyCode.SHIFT.equals(details.getKeyCode())) {
                    shiftPressed.set(true);
                    popup.setCaption(TaskLocalize.popupTitleMergeWithCurrentContext().get());
                    return true;
                }
                if (KeyCode.ENTER.equals(details.getKeyCode()) && details.withShift()) {
                    listPopup.handleSelect(true);
                    return true;
                }
                return false;
            }

            @Override
            @RequiredUIAccess
            public boolean keyReleased(ListPopup listPopup, KeyboardInputDetails details) {
                if (KeyCode.SHIFT.equals(details.getKeyCode())) {
                    shiftPressed.set(false);
                    popup.setCaption(TaskLocalize.popupTitleSwitchToTask().get());
                    return true;
                }
                return false;
            }
        });
        return popup;
    }

    private static ActionGroup createActionsStep(
        final List<TaskListItem> tasks,
        final Project project,
        final SimpleReference<Boolean> shiftPressed
    ) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        final TaskManager manager = TaskManager.getManager(project);
        final LocalTask task = tasks.get(0).getTask();
        if (tasks.size() == 1 && task != null) {
            group.add(new AnAction(TaskLocalize.actionSwitchToText()) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent e) {
                    manager.activateTask(task, !shiftPressed.get());
                }
            });
        }
        AnAction remove = new AnAction(TaskLocalize.actionRemoveText()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                for (TaskListItem item : tasks) {
                    LocalTask itemTask = item.getTask();
                    if (itemTask != null) {
                        removeTask(project, itemTask, manager);
                    }
                }
            }
        };
        group.add(remove);

        return group.build();
    }

    
    private static List<TaskListItem> createPopupActionGroup(
        Project project,
        final SimpleReference<Boolean> shiftPressed,
        DataContext dataContext
    ) {
        List<TaskListItem> group = new ArrayList<>();

        AnAction action = ActionManager.getInstance().getAction(GotoTaskAction.ID);
        group.add(new TaskListItem(action.getTemplatePresentation().getText(), action.getTemplatePresentation().getIcon()) {
            @Override
            @RequiredUIAccess
            void select() {
                ActionManager.getInstance()
                    .performActionDumbAware(action, AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext));
            }
        });

        final TaskManager manager = TaskManager.getManager(project);
        LocalTask activeTask = manager.getActiveTask();
        List<LocalTask> localTasks = manager.getLocalTasks();
        Collections.sort(localTasks, TaskManagerImpl.TASK_UPDATE_COMPARATOR);
        ArrayList<LocalTask> temp = new ArrayList<>();
        for (final LocalTask task : localTasks) {
            if (task == activeTask) {
                continue;
            }
            if (manager.isLocallyClosed(task)) {
                temp.add(task);
                continue;
            }

            group.add(new TaskListItem(task, group.size() == 1 ? "" : null, false) {
                @Override
                void select() {
                    manager.activateTask(task, !shiftPressed.get());
                }
            });
        }
        if (!temp.isEmpty()) {
            for (int i = 0, tempSize = temp.size(); i < Math.min(tempSize, 15); i++) {
                final LocalTask task = temp.get(i);

                group.add(new TaskListItem(task, i == 0 ? TaskLocalize.separatorRecentlyClosedTasks().get() : null, true) {
                    @Override
                    void select() {
                        manager.activateTask(task, !shiftPressed.get());
                    }
                });
            }
        }
        return group;
    }

    @RequiredUIAccess
    public static void removeTask(Project project, LocalTask task, TaskManager manager) {
        if (task.isDefault()) {
            MessageBoxes.okInfo(TaskLocalize.dialogMessageDefaultTaskCannotBeRemoved())
                .title(TaskLocalize.dialogTitleCannotRemove())
                .showAsync(project);
            return;
        }

        List<LocalChangeList> lists = ContainerUtil.mapNotNull(
            task.getChangeLists(),
            changeListInfo -> {
                LocalChangeList changeList = ChangeListManager.getInstance(project).getChangeList(changeListInfo.id);
                return changeList != null && !changeList.isDefault() ? changeList : null;
            }
        );

        if (ContainerUtil.all(lists, list -> list.getChanges().isEmpty())) {
            removeTask(project, task, manager, lists, true);
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        MessageBoxes.yesNoCancel()
            .asWarning()
            .title(TaskLocalize.dialogTitleChangelistNotEmpty())
            .text(TaskLocalize.dialogMessageChangelistAssociatedWithNotEmpty(task.getSummary()))
            .showAsync(project)
            .thenAcceptAsync(
                result -> {
                    if (result != ThreeState.UNSURE && !project.isDisposed()) {
                        removeTask(project, task, manager, lists, result == ThreeState.YES);
                    }
                },
                uiAccess
            )
            .exceptionally(error -> {
                LOG.error(error);
                return null;
            });
    }

    @RequiredUIAccess
    private static void removeTask(Project project, LocalTask task, TaskManager manager, List<LocalChangeList> lists, boolean removeChangeLists) {
        if (removeChangeLists) {
            for (LocalChangeList list : lists) {
                ChangeListManager.getInstance(project).removeChangeList(list);
            }
        }
        manager.removeTask(task);
    }
}
