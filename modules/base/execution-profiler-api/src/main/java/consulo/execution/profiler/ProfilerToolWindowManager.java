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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

/**
 * Shows the profiled processes of a project as profiling sessions, each in an editor tab or in the profiler tool window,
 * as the user configured.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ProfilerToolWindowManager {
    String TOOLWINDOW_ID = "Profiler";

    static ProfilerToolWindowManager getInstance(Project project) {
        return project.getInstance(ProfilerToolWindowManager.class);
    }

    /**
     * Adds the profiling session of the process. May be called on any thread.
     *
     * @param activate whether to open the session, or bring it to the front when it is already open, and focus it. When false,
     *                 the selected editor and tool window tab stay as they are, and the session is listed in the profiler tool
     *                 window
     */
    void addProfilerProcessTab(ProfilerProcess<?> process, boolean activate);

    /**
     * Opens the profiling session of the process and focuses it. May be called on any thread.
     */
    default void addProfilerProcessTab(ProfilerProcess<?> process) {
        addProfilerProcessTab(process, true);
    }
}
