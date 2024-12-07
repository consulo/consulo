package consulo.execution.configuration.ui;

import consulo.execution.RunnerAndConfigurationSettings;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public interface RunConfigurationSettingsEditor {
  void setOwner(SettingsEditor<RunnerAndConfigurationSettings> owner);
}
