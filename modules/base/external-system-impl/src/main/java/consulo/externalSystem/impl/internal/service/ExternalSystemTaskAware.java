package consulo.externalSystem.impl.internal.service;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import jakarta.annotation.Nonnull;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * Represents a service that exposes information about the tasks being processed. 
 * 
 * @author Denis Zhdanov
 * @since 2/8/12 1:46 PM
 */
public interface ExternalSystemTaskAware {

  /**
   * Allows to check if current service executes the target task.
   *
   * @param id  target task's id
   * @return    <code>true</code> if a task with the given id is executed at the moment by the current service;
   *            <code>false</code> otherwise
   * @throws RemoteException      as required by RMI
   */
  boolean isTaskInProgress(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  /**
   * Allows to cancel the target task by the current service.
   *
   *
   * @param id  target task's id
   * @return    <code>true</code> if a task was successfully canceled;
   *            <code>false</code> otherwise
   * @throws RemoteException      as required by RMI
   */
  boolean cancelTask(@Nonnull ExternalSystemTaskId id) throws RemoteException;

  /**
   * Allows to ask current service for all tasks being executed at the moment.  
   *
   * @return      ids of all tasks being executed at the moment grouped by type
   * @throws RemoteException      as required by RMI
   */
  @Nonnull
  Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException;
}
