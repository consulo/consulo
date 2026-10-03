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
package consulo.execution.profiler.live;

/**
 * The state of a thread at one point in time, as drawn on the thread timeline.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public enum ProfilerThreadState {
    /**
     * Running, or ready to run.
     */
    RUNNING,
    /**
     * Running native code.
     */
    RUNNING_NATIVE,
    /**
     * Sleeping, or waiting with a timeout.
     */
    SLEEPING,
    /**
     * Waiting without a timeout, for example on a condition or another thread.
     */
    WAITING,
    /**
     * Blocked on a monitor or lock.
     */
    BLOCKED,
    /**
     * Ended; nothing more is reported for the thread.
     */
    TERMINATED
}
