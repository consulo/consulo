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
package consulo.execution.profiler;

import consulo.application.Application;
import consulo.execution.executor.DefaultExecutorGroup;
import consulo.execution.executor.Executor;
import org.jspecify.annotations.Nullable;

/**
 * The "Profile with" executor group: one child executor per stored profiler configuration.
 * <p>
 * The child executor of a launch identifies the configuration: pass its id to {@link #getRegisteredSettings(String)}.
 * Child executor ids last only for the session and are never stored.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class DefaultProfilerExecutorGroup extends DefaultExecutorGroup<ProfilerExecutorSettings> {
    /**
     * @return the registered group, or null when the profiler is not installed
     */
    public static @Nullable DefaultProfilerExecutorGroup getInstance() {
        return Application.get().getExtensionPoint(Executor.class).findExtension(DefaultProfilerExecutorGroup.class);
    }
}
