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
package consulo.task.impl.internal.timeTracking;

import consulo.application.dumb.DumbAware;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.event.TaskListenerAdapter;
import consulo.task.icon.TaskIconGroup;
import consulo.task.impl.internal.action.GotoTaskAction;
import consulo.task.impl.internal.action.SwitchTaskAction;
import consulo.task.impl.internal.issue.IssueWorkLogDialogDescriptor;
import consulo.task.issue.IssueWorkLogSupport;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class LocalTasksPanel implements Disposable {
    private final Project myProject;
    private final TaskManager myTaskManager;
    private final TimeTrackingManager myTimeTrackingManager;
    private final MutableFlatDataModel<LocalTask> myModel = FlatDataModel.of(List.of());
    private final Table<LocalTask> myTable;
    private final DockLayout myRoot;
    private final ScheduledFuture<?> myTimer;

    @RequiredUIAccess
    public LocalTasksPanel(Project project, boolean vertical) {
        myProject = project;
        myTaskManager = TaskManager.getManager(project);
        myTimeTrackingManager = TimeTrackingManager.getInstance(project);

        myTable = Table.create(myModel);
        myTable.addColumn(TaskLocalize.toolwindowLocalTasksColumnTask(), task -> task).setRenderer((presentation, item) -> {
            LocalTask task = item.getValue();
            if (task == null) {
                return;
            }
            boolean closed = task.isClosed() || myTaskManager.isLocallyClosed(task);
            boolean active = task.isActive();
            boolean running = myTimeTrackingManager.getState().autoMode ? active : active && task.isRunning();
            presentation.withIcon(running
                ? ImageEffects.layered(task.getIcon(), PlatformIconGroup.nodesRunnablemark())
                : closed && !active ? ImageEffects.transparent(task.getIcon()) : task.getIcon());
            presentation.append(task.getPresentableName(), getAttribute(closed, active));
        }).setWidth(400);
        myTable.addColumn(TaskLocalize.toolwindowLocalTasksColumnTime(), task -> task).setRenderer((presentation, item) -> {
            LocalTask task = item.getValue();
            if (task == null) {
                return;
            }
            boolean closed = task.isClosed() || myTaskManager.isLocallyClosed(task);
            presentation.append(getTimeText(task), getAttribute(closed, task.isActive()));
        }).setWidth(150);

        DefaultActionGroup group = new DefaultActionGroup();
        AnAction gotoTask = ActionManager.getInstance().getAction(GotoTaskAction.ID);
        if (gotoTask != null) {
            group.add(gotoTask);
        }
        group.add(new RemoveAction());
        group.add(new ShowClosedAction());
        group.add(new AutoModeAction());
        group.add(new StartStopAction());
        group.add(new PostWorkItemAction());
        group.add(new ShowFromLastPostAction());
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "TaskLocalTasks",
            group,
            vertical ? ActionToolbar.Style.VERTICAL : ActionToolbar.Style.HORIZONTAL
        );
        toolbar.setTargetUIComponent(myTable);

        DockLayout toolbarArea = DockLayout.create(Space.NONE);
        toolbarArea.center(toolbar.getUIComponent());
        myRoot = DockLayout.create(Space.NONE);
        if (vertical) {
            toolbarArea.borderBuilder().rightSet().apply();
            myRoot.left(toolbarArea);
        }
        else {
            myRoot.top(toolbarArea);
        }
        myRoot.center(ScrollableLayout.create(myTable));

        updateTable();

        UIAccess uiAccess = UIAccess.current();
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskDeactivated(LocalTask task) {
                uiAccess.execute(LocalTasksPanel.this::refreshRows);
            }

            @Override
            public void taskActivated(LocalTask task) {
                uiAccess.execute(LocalTasksPanel.this::refreshRows);
            }

            @Override
            public void taskAdded(LocalTask task) {
                uiAccess.execute(LocalTasksPanel.this::updateTable);
            }

            @Override
            public void taskRemoved(LocalTask task) {
                uiAccess.execute(LocalTasksPanel.this::updateTable);
            }
        }, this);

        myTimer = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            () -> uiAccess.execute(this::refreshRows),
            TimeTrackingManager.TIME_TRACKING_TIME_UNIT,
            TimeTrackingManager.TIME_TRACKING_TIME_UNIT,
            TimeUnit.MILLISECONDS
        );
    }

    public Component getComponent() {
        return myRoot;
    }

    private static TextAttribute getAttribute(boolean closed, boolean active) {
        if (active) {
            return TextAttribute.REGULAR_BOLD;
        }
        return closed ? TextAttribute.GRAYED : TextAttribute.REGULAR;
    }

    private long getTimeSpent(LocalTask task) {
        return myTimeTrackingManager.getState().showSpentTimeFromLastPost ? task.getTimeSpentFromLastPost() : task.getTotalTimeSpent();
    }

    private String getTimeText(LocalTask task) {
        long timeSpent = getTimeSpent(task);
        return task.isActive() ? formatDuration(timeSpent) : DateFormatUtil.formatDuration(timeSpent);
    }

    private static String formatDuration(long milliseconds) {
        long second = 1000;
        long minute = 60 * second;
        long hour = 60 * minute;
        long day = 24 * hour;
        long days = milliseconds / day;
        String daysString = days != 0 ? days + "d " : "";
        return daysString + String.format("%d:%02d:%02d", milliseconds % day / hour, milliseconds % hour / minute, milliseconds % minute / second);
    }

    @RequiredUIAccess
    private void updateTable() {
        List<LocalTask> tasks = new ArrayList<>();
        for (LocalTask task : myTaskManager.getLocalTasks()) {
            if (task.isActive() || (task.getTotalTimeSpent() != 0
                && (myTimeTrackingManager.getState().showClosedTasks || !myTaskManager.isLocallyClosed(task)))) {
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.comparing(LocalTask::getUpdated, Comparator.nullsLast(Comparator.reverseOrder())));
        myModel.replaceAll(tasks);
    }

    @RequiredUIAccess
    private void refreshRows() {
        for (LocalTask task : myModel) {
            myModel.update(task);
        }
    }

    @Override
    public void dispose() {
        myTimer.cancel(false);
    }

    private class RemoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveAction() {
            super(TaskLocalize.toolwindowLocalTasksActionRemove(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            for (LocalTask task : myTable.getSelectedItems()) {
                SwitchTaskAction.removeTask(myProject, task, myTaskManager);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myTable.getSelectedItems().isEmpty());
        }
    }

    private class ShowClosedAction extends ToggleAction implements DumbAware {
        private ShowClosedAction() {
            super(TaskLocalize.toolwindowLocalTasksActionShowClosed(), LocalizeValue.empty(), PlatformIconGroup.actionsChecked());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myTimeTrackingManager.getState().showClosedTasks;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myTimeTrackingManager.getState().showClosedTasks = state;
            updateTable();
        }
    }

    private class AutoModeAction extends ToggleAction implements DumbAware {
        private AutoModeAction() {
            super(TaskLocalize.toolwindowLocalTasksActionAutoMode(), LocalizeValue.empty(), PlatformIconGroup.actionsRerun());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myTimeTrackingManager.getState().autoMode;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myTimeTrackingManager.setAutoMode(state);
            updateTable();
        }
    }

    private class StartStopAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            LocalTask activeTask = myTaskManager.getActiveTask();
            activeTask.setRunning(!activeTask.isRunning());
        }

        @Override
        public void update(AnActionEvent e) {
            boolean running = !myTimeTrackingManager.getState().autoMode && myTaskManager.getActiveTask().isRunning();
            e.getPresentation().setEnabled(!myTimeTrackingManager.getState().autoMode);
            e.getPresentation().setIcon(running ? TaskIconGroup.stoptimer() : TaskIconGroup.starttimer());
            e.getPresentation().setText(running ? TaskLocalize.toolwindowLocalTasksActionStop() : TaskLocalize.toolwindowLocalTasksActionStart());
        }
    }

    private class PostWorkItemAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private PostWorkItemAction() {
            super(TaskLocalize.toolwindowLocalTasksActionPost(), LocalizeValue.empty(), PlatformIconGroup.actionsExport());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            LocalTask task = myTable.getSelectedItem();
            IssueWorkLogSupport support = getSupport(task);
            if (task == null || support == null) {
                return;
            }
            UIAccess uiAccess = UIAccess.current();
            IssueWorkLogDialogDescriptor.showAsync(TaskLocalize.toolwindowLocalTasksActionPost(), formatWorkLogTime(task.getTimeSpentFromLastPost()), "")
                .thenAccept(result -> {
                    if (result == null) {
                        return;
                    }
                    support.logWork(task, result.timeSpent(), result.comment()).whenCompleteAsync((value, error) -> {
                        if (error != null) {
                            MessageBoxes.okError(error.getCause() != null ? error.getCause() : error).showAsync();
                        }
                        else {
                            task.setLastPost(new Date());
                            refreshRows();
                        }
                    }, uiAccess);
                });
        }

        @Override
        public void update(AnActionEvent e) {
            boolean visible = false;
            for (TaskRepository repository : myTaskManager.getAllRepositories()) {
                if (repository.getIssueTracker().getSupport(IssueWorkLogSupport.class) != null) {
                    visible = true;
                }
            }
            e.getPresentation().setVisible(visible);
            e.getPresentation().setEnabled(getSupport(myTable.getSelectedItem()) != null);
        }

        private static IssueWorkLogSupport getSupport(LocalTask task) {
            TaskRepository repository = task == null ? null : task.getRepository();
            return repository == null ? null : repository.getIssueTracker().getSupport(IssueWorkLogSupport.class);
        }
    }

    private static String formatWorkLogTime(long milliseconds) {
        long minutes = milliseconds / 60000;
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private class ShowFromLastPostAction extends ToggleAction implements DumbAware {
        private ShowFromLastPostAction() {
            super(TaskLocalize.toolwindowLocalTasksActionFromLastPost(), LocalizeValue.empty(), TaskIconGroup.clock());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myTimeTrackingManager.getState().showSpentTimeFromLastPost;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myTimeTrackingManager.getState().showSpentTimeFromLastPost = state;
            refreshRows();
        }
    }
}
