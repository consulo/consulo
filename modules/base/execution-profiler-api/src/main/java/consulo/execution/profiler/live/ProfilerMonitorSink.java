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

import java.time.Instant;

/**
 * Receives the live values of a monitored process. A profiler calls it from its own background threads; the sink moves the
 * values to the UI itself.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerMonitorSink {
    /**
     * Reports the value of a metric at a point in time.
     */
    void metric(ProfilerMetric metric, Instant time, double value);

    /**
     * Reports the state of a thread from a point in time on. A thread first seen here is added to the thread timeline.
     */
    void threadState(long threadId, String threadName, Instant time, ProfilerThreadState state);
}
