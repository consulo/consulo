/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.internal;

import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.util.dataholder.Key;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-06
 */
public abstract class ConfigurationSettingsEditorWrapper extends SettingsEditor<RunnerAndConfigurationSettings> {
    public static Key<ConfigurationSettingsEditorWrapper> CONFIGURATION_EDITOR_KEY = Key.create("ConfigurationSettingsEditor");

    public abstract List<BeforeRunTask> getStepsBeforeLaunch();

    public abstract void addBeforeLaunchStep(BeforeRunTask<?> task);
}
