package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.*;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Provides gradle tasks monitoring and management facilities.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 2/8/12 1:52 PM
 */
@Singleton
public class ExternalSystemProcessingManager implements ExternalSystemTaskNotificationListener, Disposable {

  /**
   * We receive information about the tasks being enqueued to the slave processes which work directly with external systems here.
   * However, there is a possible situation when particular task has been sent to execution but remote side has not been responding
   * for a while. There at least two possible explanations then:
   * <pre>
   * <ul>
   *   <li>the task is still in progress (e.g. great number of libraries is being downloaded);</li>
   *   <li>remote side has fallen (uncaught exception; manual slave process kill etc);</li>
   * </ul>
   * </pre>
   * We need to distinguish between them, so, we perform 'task pings' if any task is executed too long. Current constant holds
   * criteria of 'too long execution'.
   */
  private static final long TOO_LONG_EXECUTION_MS = TimeUnit.SECONDS.toMillis(10);

  @Nonnull
  private final ConcurrentMap<ExternalSystemTaskId, Long> myTasksInProgress = ContainerUtil.newConcurrentMap();
  @Nonnull
  private final ConcurrentMap<ExternalSystemTaskId, ExternalSystemTask> myTasksDetails = ContainerUtil.newConcurrentMap();
  @Nonnull
  private final Alarm                                     myAlarm           = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

  @Nonnull
  private final ExternalSystemFacadeManager               myFacadeManager;
  @Nonnull
  private final ExternalSystemProgressNotificationManager myProgressNotificationManager;

  @Inject
  public ExternalSystemProcessingManager(@Nonnull ExternalSystemFacadeManager facadeManager,
                                         @Nonnull ExternalSystemProgressNotificationManager notificationManager)
  {
    myFacadeManager = facadeManager;
    myProgressNotificationManager = notificationManager;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    notificationManager.addNotificationListener(this);
  }

  @Override
  public void dispose() {
    myProgressNotificationManager.removeNotificationListener(this);
    myAlarm.cancelAllRequests();
  }

  /**
   * Allows to check if any task of the given type is being executed at the moment.  
   *
   * @param type  target task type
   * @return      <code>true</code> if any task of the given type is being executed at the moment;
   *              <code>false</code> otherwise
   */
  public boolean hasTaskOfTypeInProgress(@Nonnull ExternalSystemTaskType type, @Nonnull Project project) {
    String projectId = ExternalSystemTaskId.getProjectId(project);
    for (ExternalSystemTaskId id : myTasksInProgress.keySet()) {
      if (type.equals(id.getType()) && projectId.equals(id.getIdeProjectId())) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public ExternalSystemTask findTask(@Nonnull ExternalSystemTaskType type,
                                     @Nonnull ProjectSystemId projectSystemId,
                                     @Nonnull final String externalProjectPath) {
    for(ExternalSystemTask task : myTasksDetails.values()) {
      if(task instanceof AbstractExternalSystemTask) {
        AbstractExternalSystemTask externalSystemTask = (AbstractExternalSystemTask)task;
        if(externalSystemTask.getId().getType() == type &&
           externalSystemTask.getExternalSystemId().getId().equals(projectSystemId.getId()) &&
           externalSystemTask.getExternalProjectPath().equals(externalProjectPath)){
          return task;
        }
      }
    }

    return null;
  }

  public void add(@Nonnull ExternalSystemTask task) {
    myTasksDetails.put(task.getId(), task);
  }

  public void release(@Nonnull ExternalSystemTaskId id) {
    myTasksDetails.remove(id);
  }

  @Override
  public void onQueued(@Nonnull ExternalSystemTaskId id) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
    if (myAlarm.getActiveRequestCount() <= 0) {
      myAlarm.addRequest(new Runnable() {
        @Override
        public void run() {
          update();
        }
      }, TOO_LONG_EXECUTION_MS);
    }
  }

  @Override
  public void onStart(@Nonnull ExternalSystemTaskId id) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
  }

  @Override
  public void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event) {
    myTasksInProgress.put(event.getId(), System.currentTimeMillis() + TOO_LONG_EXECUTION_MS); 
  }

  @Override
  public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
  }

  @Override
  public void onEnd(@Nonnull ExternalSystemTaskId id) {
    myTasksInProgress.remove(id);
    if (myTasksInProgress.isEmpty()) {
      myAlarm.cancelAllRequests();
    }
  }

  @Override
  public void onSuccess(@Nonnull ExternalSystemTaskId id) {
  }

  @Override
  public void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e) {
  }

  public void update() {
    long delay = TOO_LONG_EXECUTION_MS;
    Map<ExternalSystemTaskId, Long> newState = ContainerUtilRt.newHashMap();

    Map<ExternalSystemTaskId, Long> currentState = ContainerUtilRt.newHashMap(myTasksInProgress);
    if (currentState.isEmpty()) {
      return;
    }
    
    for (Map.Entry<ExternalSystemTaskId, Long> entry : currentState.entrySet()) {
      long diff = System.currentTimeMillis() - entry.getValue();
      if (diff > 0) {
        delay = Math.min(delay, diff);
        newState.put(entry.getKey(), entry.getValue());
      }
      else {
        // Perform explicit check on whether the task is still alive.
        if (myFacadeManager.isTaskActive(entry.getKey())) {
          newState.put(entry.getKey(), System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
        }
      }
    }
    
    myTasksInProgress.clear();
    myTasksInProgress.putAll(newState);

    if (!newState.isEmpty()) {
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(new Runnable() {
        @Override
        public void run() {
          update(); 
        }
      }, delay);
    }
  }
}
