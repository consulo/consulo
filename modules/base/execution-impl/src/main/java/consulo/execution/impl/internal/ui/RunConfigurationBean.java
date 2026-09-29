/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.impl.internal.ui;

import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
final class RunConfigurationBean {
    private final RunnerAndConfigurationSettings mySettings;
    private final boolean myShared;
    private final List<BeforeRunTask> myStepsBeforeLaunch;
    private final @Nullable SingleConfigurationConfigurable<?> myConfigurable;

    RunConfigurationBean(RunnerAndConfigurationSettings settings, boolean shared, List<BeforeRunTask> stepsBeforeLaunch) {
        mySettings = settings;
        myShared = shared;
        myStepsBeforeLaunch = Collections.unmodifiableList(stepsBeforeLaunch);
        myConfigurable = null;
    }

    RunConfigurationBean(SingleConfigurationConfigurable<?> configurable) {
        myConfigurable = configurable;
        mySettings = configurable.getSettings();
        myShared = configurable.isStoreProjectConfiguration();
        myStepsBeforeLaunch = configurable.getStepsBeforeLaunch();
    }

    RunnerAndConfigurationSettings getSettings() {
        return mySettings;
    }

    boolean isShared() {
        return myShared;
    }

    List<BeforeRunTask> getStepsBeforeLaunch() {
        return myStepsBeforeLaunch;
    }

    @Nullable SingleConfigurationConfigurable<?> getConfigurable() {
        return myConfigurable;
    }

    @Override
    public String toString() {
        return String.valueOf(mySettings);
    }
}
