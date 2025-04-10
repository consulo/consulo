package consulo.externalSystem.service.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;

import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 11/10/11 12:04 PM
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ExternalSystemProgressNotificationManager {

  /**
   * Allows to register given listener to listen events from all tasks.
   *
   * @param listener  listener to register
   * @return          <code>true</code> if given listener was not registered before for the given key;
   *                  <code>false</code> otherwise
   */
  boolean addNotificationListener(@Nonnull ExternalSystemTaskNotificationListener listener);

  /**
   * Allows to register given listener within the current manager for listening events from the task with the target id. 
   *
   * @param taskId    target task's id
   * @param listener  listener to register
   * @return          <code>true</code> if given listener was not registered before for the given key;
   *                  <code>false</code> otherwise
   */
  boolean addNotificationListener(@Nonnull ExternalSystemTaskId taskId, @Nonnull ExternalSystemTaskNotificationListener listener);

  /**
   * Allows to de-register given listener from the current manager
   *
   * @param listener  listener to de-register
   * @return          <code>true</code> if given listener was successfully de-registered;
   *                  <code>false</code> if given listener was not registered before
   */
  boolean removeNotificationListener(@Nonnull ExternalSystemTaskNotificationListener listener);
}
