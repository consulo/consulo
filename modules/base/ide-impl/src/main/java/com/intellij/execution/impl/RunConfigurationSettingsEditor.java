package com.intellij.execution.impl;

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ui.SettingsEditor;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public interface RunConfigurationSettingsEditor {

  void setOwner(SettingsEditor<RunnerAndConfigurationSettings> owner);
}
