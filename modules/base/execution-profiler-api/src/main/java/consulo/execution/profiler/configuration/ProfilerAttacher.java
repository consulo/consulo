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

import consulo.execution.attach.XAttachHost;
import consulo.execution.profiler.ProfilerProcess;
import consulo.platform.ProcessInfo;
import consulo.project.Project;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Attaches a configuration to a process which is already running on an attach host.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerAttacher {
    /**
     * @return whether this configuration can attach to the process. Called on a background thread.
     */
    boolean isApplicable(XAttachHost host, ProcessInfo process);

    /**
     * @return what attaching to this process allows, which may differ between processes of the same runtime.
     * Called on a background thread.
     */
    Set<ProfilerFeature> getFeatures(XAttachHost host, ProcessInfo process);

    /**
     * Attaches in the background.
     *
     * @return the attached process, or a failed future when attaching was refused or failed
     */
    CompletableFuture<ProfilerProcess<?>> attach(Project project, XAttachHost host, ProcessInfo process);
}
