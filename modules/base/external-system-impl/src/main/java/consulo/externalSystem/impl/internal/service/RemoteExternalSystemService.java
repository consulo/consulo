package consulo.externalSystem.impl.internal.service;

import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;

/**
 * Generic interface with common functionality for all remote services that work with external system.
 *
 * @author Denis Zhdanov
 * @since 8/9/11 3:19 PM
 */
public interface RemoteExternalSystemService<S extends ExternalSystemExecutionSettings> extends ExternalSystemTaskAware {

  void setSettings(S settings);

  void setNotificationListener(ExternalSystemTaskNotificationListener notificationListener);
}
