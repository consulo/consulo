package consulo.externalSystem.impl.internal.service;

import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemTaskManager;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Serves as a facade for working with external system which might be located at an external (non-ide) process.
 *
 * @author Denis Zhdanov
 * @since 8/8/11 10:52 AM
 */
public interface RemoteExternalSystemFacade<S extends ExternalSystemExecutionSettings> extends ExternalSystemTaskAware {

  /** <a href="http://en.wikipedia.org/wiki/Null_Object_pattern">Null object</a> for {@link RemoteExternalSystemFacade}. */
  RemoteExternalSystemFacade<?> NULL_OBJECT = new RemoteExternalSystemFacade<ExternalSystemExecutionSettings>() {

    @Override
    public RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings> getResolver() {
      return RemoteExternalSystemProjectResolver.NULL_OBJECT;
    }

    @Override
    public RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings> getTaskManager() {
      return RemoteExternalSystemTaskManager.NULL_OBJECT;
    }

    @Override
    public void applySettings(ExternalSystemExecutionSettings settings) {
    }

    @Override
    public void applyProgressManager(RemoteExternalSystemProgressNotificationManager progressManager) {
    }

    @Override
    public boolean isTaskInProgress(ExternalSystemTaskId id) {
      return false;
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId id) {
      return false;
    }

    @Override
    public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
      return Collections.emptyMap();
    }
  };

  RemoteExternalSystemProjectResolver<S> getResolver();

  RemoteExternalSystemTaskManager<S> getTaskManager();

  void applySettings(S settings);

  void applyProgressManager(RemoteExternalSystemProgressNotificationManager progressManager);
}
