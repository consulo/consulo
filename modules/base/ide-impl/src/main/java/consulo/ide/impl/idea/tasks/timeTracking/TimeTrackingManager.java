package consulo.ide.impl.idea.tasks.timeTracking;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.IdeEventQueue;
import consulo.ide.impl.idea.openapi.util.Disposer;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.WorkItem;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

/**
 * User: Evgeny.Zakrevsky
 * Date: 11/19/12
 */

@Singleton
@State(name = "TimeTrackingManager", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class TimeTrackingManager implements PersistentStateComponent<TimeTrackingManager.Config>, Disposable {
  public static final int TIME_TRACKING_TIME_UNIT = 1000;

  private final Project myProject;
  private final TaskManager myTaskManager;
  private final Config myConfig = new Config();
  private Timer myTimeTrackingTimer;
  private final Alarm myIdleAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private Runnable myActivityListener;
  private LocalTask myLastActiveTask;

  @Inject
  public TimeTrackingManager(Project project, TaskManager taskManager) {
    myProject = project;
    myTaskManager = taskManager;
  }

  public static TimeTrackingManager getInstance(Project project) {
    return project.getComponent(TimeTrackingManager.class);
  }

  private void startTimeTrackingTimer() {
    if (!myTimeTrackingTimer.isRunning()) {
      myTimeTrackingTimer.start();
    }

    myIdleAlarm.cancelAllRequests();
    myIdleAlarm.addRequest(new Runnable() {
      @Override
      public void run() {
        if (myTimeTrackingTimer.isRunning()) {
          myTimeTrackingTimer.stop();
        }
      }
    }, getState().suspendDelayInSeconds * 1000);
  }

  public void updateTimeTrackingToolWindow() {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.TASKS);
    if (isTimeTrackingToolWindowAvailable()) {
      if (toolWindow == null) {
        toolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(ToolWindowId.TASKS, true, ToolWindowAnchor.RIGHT, myProject, true);
        new TasksToolWindowFactory().createToolWindowContent(myProject, toolWindow);
      }
      final ToolWindow finalToolWindow = toolWindow;
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          finalToolWindow.setAvailable(true, null);
          finalToolWindow.show(null);
          finalToolWindow.activate(null);
        }
      });
    }
    else {
      if (toolWindow != null) {
        final ToolWindow finalToolWindow = toolWindow;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            finalToolWindow.setAvailable(false, null);
          }
        });
      }
    }
  }

  public boolean isTimeTrackingToolWindowAvailable() {
    return getState().enabled;
  }

  @Override
  public void afterLoadState() {
    if (myProject.isDefault()) {
      return;
    }

    myTimeTrackingTimer = UIUtil.createNamedTimer("TaskManager time tracking", TIME_TRACKING_TIME_UNIT, new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final LocalTask activeTask = myTaskManager.getActiveTask();
        if (myLastActiveTask != activeTask) {
          activeTask.addWorkItem(new WorkItem(new Date()));
        }
        if (getState().autoMode) {
          final WorkItem lastWorkItem = activeTask.getWorkItems().get(activeTask.getWorkItems().size() - 1);
          lastWorkItem.duration += TIME_TRACKING_TIME_UNIT;
          getState().totallyTimeSpent += TIME_TRACKING_TIME_UNIT;
        }
        else {
          if (activeTask.isRunning()) {
            final WorkItem lastWorkItem = activeTask.getWorkItems().get(activeTask.getWorkItems().size() - 1);
            lastWorkItem.duration += TIME_TRACKING_TIME_UNIT;
            getState().totallyTimeSpent += TIME_TRACKING_TIME_UNIT;
          }
        }
        myLastActiveTask = activeTask;
      }
    });
    StartupManager.getInstance(myProject).registerStartupActivity(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            startTimeTrackingTimer();
          }
        });
      }
    });

    myActivityListener = new Runnable() {
      @Override
      public void run() {
        final IdeFrame frame = (IdeFrame)IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        if (frame == null) {
          return;
        }
        final Project project = frame.getProject();
        if (project == null || !myProject.equals(project)) {
          return;
        }
        startTimeTrackingTimer();
      }
    };
    if (getState().autoMode) {
      addAWTListener();
    }
  }

  public void setAutoMode(final boolean on) {
    final boolean oldState = getState().autoMode;
    if (on != oldState) {
      getState().autoMode = on;
      if (on) {
        addAWTListener();
      }
      else {
        IdeEventQueue.getInstance().removeActivityListener(myActivityListener);
        myIdleAlarm.cancelAllRequests();
        if (!myTimeTrackingTimer.isRunning()) {
          myTimeTrackingTimer.start();
        }
      }
    }
  }

  private void addAWTListener() {
    if (!Application.get().isSwingApplication()) {
      return;
    }
    IdeEventQueue.getInstance().addActivityListener(myActivityListener, myProject);
  }

  @Override
  public void dispose() {
    if (myTimeTrackingTimer != null) {
      myTimeTrackingTimer.stop();
    }
    myIdleAlarm.cancelAllRequests();
    Disposer.dispose(myIdleAlarm);
  }

  @Nonnull
  @Override
  public TimeTrackingManager.Config getState() {
    return myConfig;
  }

  @Override
  public void loadState(final TimeTrackingManager.Config state) {
    XmlSerializerUtil.copyBean(state, myConfig);
  }

  public static class Config {
    public boolean enabled = false;
    public long totallyTimeSpent = 0;
    public int suspendDelayInSeconds = 600;
    public boolean autoMode = true;
    public boolean showClosedTasks = true;
    public boolean showSpentTimeFromLastPost = false;
  }
}
