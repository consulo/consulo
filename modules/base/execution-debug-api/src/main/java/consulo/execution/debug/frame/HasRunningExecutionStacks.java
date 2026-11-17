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
package consulo.execution.debug.frame;

import org.jspecify.annotations.Nullable;

/**
 * Implemented by a {@link consulo.execution.debug.XDebugProcess} which can list its live threads - the session gets the Threads
 * view, which shows them also while the process is running.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public interface HasRunningExecutionStacks {
    /**
     * Provide execution stacks corresponding to all the live threads in the debug process to the {@code container}. A process with
     * thread groups adds them by {@link XSuspendContext.XExecutionStackGroupContainer#addExecutionStackGroups}.
     *
     * @param suspendContext the current suspend context of the session, if it is suspended
     */
    void computeRunningExecutionStacks(XSuspendContext.XExecutionStackGroupContainer container, @Nullable XSuspendContext suspendContext);
}
