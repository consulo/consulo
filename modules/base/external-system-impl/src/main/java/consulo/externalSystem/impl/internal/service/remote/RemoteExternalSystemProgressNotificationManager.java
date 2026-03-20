package consulo.externalSystem.impl.internal.service.remote;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationEvent;

/**
 * Defines interface for the entity that manages notifications about progress of long-running operations performed at external system side.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 *
 * @author Denis Zhdanov
 * @since 11/10/11 9:03 AM
 */
public interface RemoteExternalSystemProgressNotificationManager {

  RemoteExternalSystemProgressNotificationManager NULL_OBJECT = new RemoteExternalSystemProgressNotificationManager() {
    @Override
    public void onQueued(ExternalSystemTaskId id) {
    }

    @Override
    public void onStart(ExternalSystemTaskId id) {
    }

    @Override
    public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
    }

    @Override
    public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
    }

    @Override
    public void onEnd(ExternalSystemTaskId id) {
    }

    @Override
    public void onSuccess(ExternalSystemTaskId id) {
    }

    @Override
    public void onFailure(ExternalSystemTaskId id, Exception e) {
    }
  };

  void onQueued(ExternalSystemTaskId id);

  void onStart(ExternalSystemTaskId id);

  void onStatusChange(ExternalSystemTaskNotificationEvent event);

  void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut);

  void onEnd(ExternalSystemTaskId id);

  void onSuccess(ExternalSystemTaskId id);

  void onFailure(ExternalSystemTaskId id, Exception e);
}
