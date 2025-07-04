package consulo.ide.impl.idea.tasks.timeTracking;

import consulo.application.util.DateFormatUtil;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.tasks.actions.GotoTaskAction;
import consulo.ide.impl.idea.tasks.actions.SwitchTaskAction;
import consulo.util.collection.ContainerUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.event.TaskListenerAdapter;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Comparator;

/**
 * @author evgeny.zakrevsky
 * @since 2012-11-08
 */
public class TasksToolWindowPanel extends SimpleToolWindowPanel implements Disposable {

    private final ListTableModel<LocalTask> myTableModel;
    private final TimeTrackingManager myTimeTrackingManager;
    private final Project myProject;
    private Timer myTimer;
    private final TableView<LocalTask> myTable;
    private final TaskManager myTaskManager;

    public TasksToolWindowPanel(Project project, boolean vertical) {
        super(vertical);
        myProject = project;
        myTimeTrackingManager = TimeTrackingManager.getInstance(project);
        myTaskManager = TaskManager.getManager(project);

        myTable = new TableView<>(createListModel());
        myTableModel = myTable.getListTableModel();
        updateTable();

        setContent(ScrollPaneFactory.createScrollPane(myTable, true));
        setToolbar(createToolbar());

        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskDeactivated(LocalTask task) {
                myTable.repaint();
            }

            @Override
            public void taskActivated(LocalTask task) {
                myTable.repaint();
            }

            @Override
            public void taskAdded(LocalTask task) {
                updateTable();
            }

            @Override
            public void taskRemoved(LocalTask task) {
                updateTable();
            }
        }, this);

        myTimer = new Timer(TimeTrackingManager.TIME_TRACKING_TIME_UNIT, e -> myTable.repaint());
        myTimer.start();
    }

    private static SimpleTextAttributes getAttributes(boolean isClosed, boolean isActive, boolean isSelected) {
        return new SimpleTextAttributes(
            isActive ? SimpleTextAttributes.STYLE_BOLD : SimpleTextAttributes.STYLE_PLAIN,
            isSelected
                ? UIUtil.getTableSelectionForeground()
                : isClosed && !isActive
                ? UIUtil.getLabelDisabledForeground()
                : UIUtil.getTableForeground()
        );
    }

    private static String formatDuration(long milliseconds) {
        int second = 1000;
        int minute = 60 * second;
        int hour = 60 * minute;
        int day = 24 * hour;
        int days = (int)milliseconds / day;
        String daysString = days != 0 ? days + "d " : "";

        return daysString + String.format(
            "%d:%02d:%02d",
            milliseconds % day / hour,
            milliseconds % hour / minute,
            milliseconds % minute / second
        );
    }

    private JComponent createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        AnAction action = ActionManager.getInstance().getAction(GotoTaskAction.ID);
        assert action instanceof GotoTaskAction;
        GotoTaskAction gotoTaskAction = (GotoTaskAction)action;
        group.add(gotoTaskAction);
        group.add(
            new AnAction("Remove Task", "Remove Task", PlatformIconGroup.generalRemove()) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    for (LocalTask localTask : myTable.getSelectedObjects()) {
                        SwitchTaskAction.removeTask(myProject, localTask, myTaskManager);
                    }
                }
            }
        );
        group.add(
            new ToggleAction("Show closed tasks", "Show closed tasks", PlatformIconGroup.actionsChecked()) {
                @Override
                public boolean isSelected(@Nonnull AnActionEvent e) {
                    return myTimeTrackingManager.getState().showClosedTasks;
                }

                @Override
                public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                    myTimeTrackingManager.getState().showClosedTasks = state;
                    updateTable();
                }
            }
        );
        group.add(new ModeToggleAction());
        group.add(new StartStopAction());

        if (timeManagementExist()) {
            group.add(
                new AnAction("Post work item to bugtracker", "Post work item to bugtracker", PlatformIconGroup.actionsExport()) {
                    @Override
                    @RequiredUIAccess
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        LocalTask localTask = myTable.getSelectedObject();
                        if (localTask == null) {
                            return;
                        }
                        new SendTimeTrackingInformationDialog(myProject, localTask).show();
                    }

                    @Override
                    public void update(@Nonnull AnActionEvent e) {
                        LocalTask localTask = myTable.getSelectedObject();
                        if (localTask == null) {
                            e.getPresentation().setEnabled(false);
                        }
                        else {
                            TaskRepository repository = localTask.getRepository();
                            e.getPresentation().setEnabled(repository != null && repository.isSupported(TaskRepository.TIME_MANAGEMENT));
                        }
                    }
                }
            );

            group.add(
                new ToggleAction(
                    "Show time spent from last post of work item",
                    "Show time spent from last post of work item",
                    TaskIconGroup.clock()
                ) {
                    @Override
                    public boolean isSelected(@Nonnull AnActionEvent e) {
                        return myTimeTrackingManager.getState().showSpentTimeFromLastPost;
                    }

                    @Override
                    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                        myTimeTrackingManager.getState().showSpentTimeFromLastPost = state;
                        myTable.repaint();
                    }
                }
            );
        }
        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, myVertical);
        actionToolBar.setTargetComponent(this);
        return actionToolBar.getComponent();
    }

    private void updateTable() {
        myTableModel.setItems(ContainerUtil.filter(
            myTaskManager.getLocalTasks(),
            task -> task.isActive()
                || (task.getTotalTimeSpent() != 0
                && (myTimeTrackingManager.getState().showClosedTasks || !myTaskManager.isLocallyClosed(task)))
        ));
    }

    private ListTableModel<LocalTask> createListModel() {
        ColumnInfo<LocalTask, String> task = new ColumnInfo<>("Task") {
            @Nullable
            @Override
            public String valueOf(LocalTask task) {
                return task.getPresentableName();
            }

            @Nullable
            @Override
            public TableCellRenderer getRenderer(LocalTask task) {
                return (table, value, isSelected, hasFocus, row, column) -> {
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.setBackground(UIUtil.getTableBackground(isSelected));
                    SimpleColoredComponent component = new SimpleColoredComponent();
                    boolean isClosed = task.isClosed() || myTaskManager.isLocallyClosed(task);
                    boolean isActive = task.isActive();
                    boolean isRunning = myTimeTrackingManager.getState().autoMode ? isActive : isActive && task.isRunning();
                    component.append((String)value, getAttributes(isClosed, isActive, isSelected));
                    component.setIcon(
                        isRunning
                            ? ImageEffects.layered(task.getIcon(), PlatformIconGroup.nodesRunnablemark())
                            : isClosed && !isActive
                            ? ImageEffects.transparent(task.getIcon())
                            : task.getIcon()
                    );
                    component.setOpaque(false);
                    panel.add(component, BorderLayout.CENTER);
                    panel.setOpaque(true);
                    return panel;
                };
            }

            @Nullable
            @Override
            public Comparator<LocalTask> getComparator() {
                return (o1, o2) -> {
                    int i = Comparing.compare(o2.getUpdated(), o1.getUpdated());
                    return i == 0 ? Comparing.compare(o2.getCreated(), o1.getCreated()) : i;
                };
            }
        };

        ColumnInfo<LocalTask, String> spentTime = new ColumnInfo<>("Time Spent") {
            @Nullable
            @Override
            public String valueOf(LocalTask task) {
                long timeSpent =
                    myTimeTrackingManager.getState().showSpentTimeFromLastPost ? task.getTimeSpentFromLastPost() : task.getTotalTimeSpent();
                if (task.isActive()) {
                    return formatDuration(timeSpent);
                }
                return DateFormatUtil.formatDuration(timeSpent);
            }

            @Nullable
            @Override
            public TableCellRenderer getRenderer(LocalTask task) {
                return (table, value, isSelected, hasFocus, row, column) -> {
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.setBackground(UIUtil.getTableBackground(isSelected));
                    SimpleColoredComponent component = new SimpleColoredComponent();
                    boolean isClosed = task.isClosed() || myTaskManager.isLocallyClosed(task);
                    boolean isActive = task.isActive();
                    component.append((String)value, getAttributes(isClosed, isActive, isSelected));
                    component.setOpaque(false);
                    panel.add(component, BorderLayout.CENTER);
                    panel.setOpaque(true);
                    return panel;
                };
            }

            @Nullable
            @Override
            public Comparator<LocalTask> getComparator() {
                return (o1, o2) -> {
                    long timeSpent1 =
                        myTimeTrackingManager.getState().showSpentTimeFromLastPost ? o1.getTimeSpentFromLastPost() : o1.getTotalTimeSpent();
                    long timeSpent2 =
                        myTimeTrackingManager.getState().showSpentTimeFromLastPost ? o2.getTimeSpentFromLastPost() : o2.getTotalTimeSpent();
                    return Comparing.compare(timeSpent1, timeSpent2);
                };
            }
        };

        return new ListTableModel<>((new ColumnInfo[]{task, spentTime}));
    }

    private boolean timeManagementExist() {
        for (TaskRepository repository : myTaskManager.getAllRepositories()) {
            if (repository.isSupported(TaskRepository.TIME_MANAGEMENT)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        myTimer.stop();
        myTimer = null;
    }

    private class StartStopAction extends AnAction {
        @Override
        public void update(@Nonnull AnActionEvent e) {
            if (myTimeTrackingManager.getState().autoMode) {
                e.getPresentation().setEnabled(false);
                e.getPresentation().setIcon(TaskIconGroup.starttimer());
                e.getPresentation().setText("Start timer for active task");
            }
            else {
                e.getPresentation().setEnabled(true);
                if (myTaskManager.getActiveTask().isRunning()) {
                    e.getPresentation().setIcon(TaskIconGroup.stoptimer());
                    e.getPresentation().setText("Stop timer for active task");
                }
                else {
                    e.getPresentation().setIcon(TaskIconGroup.starttimer());
                    e.getPresentation().setText("Start timer for active task");
                }
            }
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            LocalTask activeTask = myTaskManager.getActiveTask();
            if (activeTask.isRunning()) {
                activeTask.setRunning(false);
            }
            else {
                activeTask.setRunning(true);
            }
        }
    }

    private class ModeToggleAction extends ToggleAction {
        public ModeToggleAction() {
            super("Auto mode", "Automatic starting and stopping of timer", PlatformIconGroup.actionsRerun());
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myTimeTrackingManager.getState().autoMode;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myTimeTrackingManager.setAutoMode(state);
            updateTable();
        }
    }
}
