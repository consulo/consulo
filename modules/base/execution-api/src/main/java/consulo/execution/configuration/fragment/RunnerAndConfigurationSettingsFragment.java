package consulo.execution.configuration.fragment;

import consulo.execution.RunnerAndConfigurationSettings;

/**
 * A fragment that edits the settings around a configuration (single instance, before launch) instead of the configuration.
 */
public interface RunnerAndConfigurationSettingsFragment extends RunConfigurationFragment<RunnerAndConfigurationSettings> {
}
