package consulo.execution.configuration.ui;

import consulo.execution.RunnerAndConfigurationSettings;

/**
 * @author michael.golubev
 */
public interface RunConfigurationSettingsEditor {
  void setOwner(SettingsEditor<RunnerAndConfigurationSettings> owner);
}
