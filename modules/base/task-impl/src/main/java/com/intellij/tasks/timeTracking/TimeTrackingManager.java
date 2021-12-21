package com.intellij.tasks.timeTracking;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.*;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.timeTracking.model.WorkItem;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
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
public class TimeTrackingManager implements PersistentStateComponent<TimeTrackingManager.Config>, Disposable
{
	public static final int TIME_TRACKING_TIME_UNIT = 1000;

	private final Project myProject;
	private final TaskManager myTaskManager;
	private final Config myConfig = new Config();
	private Timer myTimeTrackingTimer;
	private final Alarm myIdleAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
	private Runnable myActivityListener;
	private LocalTask myLastActiveTask;

	@Inject
	public TimeTrackingManager(Project project, TaskManager taskManager)
	{
		myProject = project;
		myTaskManager = taskManager;
	}

	public static TimeTrackingManager getInstance(Project project)
	{
		return project.getComponent(TimeTrackingManager.class);
	}

	private void startTimeTrackingTimer()
	{
		if(!myTimeTrackingTimer.isRunning())
		{
			myTimeTrackingTimer.start();
		}

		myIdleAlarm.cancelAllRequests();
		myIdleAlarm.addRequest(new Runnable()
		{
			@Override
			public void run()
			{
				if(myTimeTrackingTimer.isRunning())
				{
					myTimeTrackingTimer.stop();
				}
			}
		}, getState().suspendDelayInSeconds * 1000);
	}

	public void updateTimeTrackingToolWindow()
	{
		ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.TASKS);
		if(isTimeTrackingToolWindowAvailable())
		{
			if(toolWindow == null)
			{
				toolWindow =
						ToolWindowManager.getInstance(myProject).registerToolWindow(ToolWindowId.TASKS, true, ToolWindowAnchor.RIGHT, myProject, true);
				new TasksToolWindowFactory().createToolWindowContent(myProject, toolWindow);
			}
			final ToolWindow finalToolWindow = toolWindow;
			ApplicationManager.getApplication().invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					finalToolWindow.setAvailable(true, null);
					finalToolWindow.show(null);
					finalToolWindow.activate(null);
				}
			});
		}
		else
		{
			if(toolWindow != null)
			{
				final ToolWindow finalToolWindow = toolWindow;
				ApplicationManager.getApplication().invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						finalToolWindow.setAvailable(false, null);
					}
				});
			}
		}
	}

	public boolean isTimeTrackingToolWindowAvailable()
	{
		return getState().enabled;
	}

	@Override
	public void afterLoadState()
	{
		if(myProject.isDefault())
		{
			return;
		}

		myTimeTrackingTimer = UIUtil.createNamedTimer("TaskManager time tracking", TIME_TRACKING_TIME_UNIT, new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				final LocalTask activeTask = myTaskManager.getActiveTask();
				if(myLastActiveTask != activeTask)
				{
					activeTask.addWorkItem(new WorkItem(new Date()));
				}
				if(getState().autoMode)
				{
					final WorkItem lastWorkItem = activeTask.getWorkItems().get(activeTask.getWorkItems().size() - 1);
					lastWorkItem.duration += TIME_TRACKING_TIME_UNIT;
					getState().totallyTimeSpent += TIME_TRACKING_TIME_UNIT;
				}
				else
				{
					if(activeTask.isRunning())
					{
						final WorkItem lastWorkItem = activeTask.getWorkItems().get(activeTask.getWorkItems().size() - 1);
						lastWorkItem.duration += TIME_TRACKING_TIME_UNIT;
						getState().totallyTimeSpent += TIME_TRACKING_TIME_UNIT;
					}
				}
				myLastActiveTask = activeTask;
			}
		});
		StartupManager.getInstance(myProject).registerStartupActivity(new Runnable()
		{
			public void run()
			{
				ApplicationManager.getApplication().invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						startTimeTrackingTimer();
					}
				});
			}
		});

		myActivityListener = new Runnable()
		{
			@Override
			public void run()
			{
				final IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
				if(frame == null)
				{
					return;
				}
				final Project project = frame.getProject();
				if(project == null || !myProject.equals(project))
				{
					return;
				}
				startTimeTrackingTimer();
			}
		};
		if(getState().autoMode)
		{
			addAWTListener();
		}
	}

	public void setAutoMode(final boolean on)
	{
		final boolean oldState = getState().autoMode;
		if(on != oldState)
		{
			getState().autoMode = on;
			if(on)
			{
				addAWTListener();
			}
			else
			{
				IdeEventQueue.getInstance().removeActivityListener(myActivityListener);
				myIdleAlarm.cancelAllRequests();
				if(!myTimeTrackingTimer.isRunning())
				{
					myTimeTrackingTimer.start();
				}
			}
		}
	}

	private void addAWTListener()
	{
		if(!Application.get().isSwingApplication())
		{
			return;
		}
		IdeEventQueue.getInstance().addActivityListener(myActivityListener, myProject);
	}

	@Override
	public void dispose()
	{
		if(myTimeTrackingTimer != null)
		{
			myTimeTrackingTimer.stop();
		}
		myIdleAlarm.cancelAllRequests();
		Disposer.dispose(myIdleAlarm);
	}

	@Nonnull
	@Override
	public TimeTrackingManager.Config getState()
	{
		return myConfig;
	}

	@Override
	public void loadState(final TimeTrackingManager.Config state)
	{
		XmlSerializerUtil.copyBean(state, myConfig);
	}

	public static class Config
	{
		public boolean enabled = false;
		public long totallyTimeSpent = 0;
		public int suspendDelayInSeconds = 600;
		public boolean autoMode = true;
		public boolean showClosedTasks = true;
		public boolean showSpentTimeFromLastPost = false;
	}
}
