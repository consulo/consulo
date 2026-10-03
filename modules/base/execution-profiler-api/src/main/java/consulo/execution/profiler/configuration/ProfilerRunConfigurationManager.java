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
package consulo.execution.profiler.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;

import java.util.List;

/**
 * Stores the profiler configurations of all types. Every configuration becomes one "Profile with" entry.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProfilerRunConfigurationManager {
    static ProfilerRunConfigurationManager getInstance() {
        return Application.get().getInstance(ProfilerRunConfigurationManager.class);
    }

    /**
     * @return the stored configurations, in order. The states are owned by the manager: copy one before editing it.
     */
    List<ProfilerConfigurationState> getConfigurations();

    /**
     * Replaces every stored configuration.
     */
    void setConfigurations(List<? extends ProfilerConfigurationState> configurations);

    /**
     * Calls the listener, on any thread, after the configurations changed.
     *
     * @return removes the listener when disposed
     */
    Disposable addChangeListener(Runnable listener);
}
