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

import consulo.disposer.Disposable;
import consulo.execution.profiler.ProfilerData;
import consulo.execution.profiler.configuration.ProfilerFeature;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A profiled process which can be watched while it runs and asked for captures on demand. Implemented by
 * {@link consulo.execution.profiler.ProfilerProcess} subclasses.
 * <p>
 * Commands run in the background; their futures complete on a background thread.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface LiveProfilerProcess {
    /**
     * @return what this process supports; commands outside it complete exceptionally
     */
    Set<ProfilerFeature> getFeatures();

    /**
     * Starts pushing live values into the sink, until the returned disposable is disposed or the process ends.
     */
    Disposable startMonitoring(ProfilerMonitorSink sink);

    CompletableFuture<?> startCpuRecording();

    /**
     * Stops the CPU recording started last.
     *
     * @return the recorded data
     */
    CompletableFuture<ProfilerData> stopCpuRecording();

    default CompletableFuture<ProfilerData> dumpHeap() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Heap dumps are not supported by " + this));
    }

    default CompletableFuture<ProfilerData> dumpThreads() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Thread dumps are not supported by " + this));
    }
}
