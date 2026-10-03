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
package consulo.sandboxPlugin.ide.profiler;

import consulo.disposer.Disposable;
import consulo.execution.profiler.live.ProfilerMetric;
import consulo.execution.profiler.live.ProfilerMonitorSink;
import consulo.execution.profiler.live.ProfilerThreadState;
import consulo.execution.profiler.model.NativeThread;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.TimeSeriesKind;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class SandProfilerMonitor implements Disposable, Runnable {
    static final ProfilerMetric PROCESS_CPU = new ProfilerMetric(
        "sand.cpu.process",
        LocalizeValue.localizeTODO("Process CPU"),
        ChartUnit.PERCENT,
        "cpu",
        TimeSeriesKind.AREA
    );
    static final ProfilerMetric SYSTEM_CPU = new ProfilerMetric(
        "sand.cpu.system",
        LocalizeValue.localizeTODO("System CPU"),
        ChartUnit.PERCENT,
        "cpu",
        TimeSeriesKind.LINE
    );
    static final ProfilerMetric HEAP_USED = new ProfilerMetric(
        "sand.heap.used",
        LocalizeValue.localizeTODO("Heap used"),
        ChartUnit.BYTES,
        "memory",
        TimeSeriesKind.AREA
    );
    static final ProfilerMetric HEAP_COMMITTED = new ProfilerMetric(
        "sand.heap.committed",
        LocalizeValue.localizeTODO("Heap committed"),
        ChartUnit.BYTES,
        "memory",
        TimeSeriesKind.LINE
    );
    static final ProfilerMetric RUNNING_THREADS = new ProfilerMetric(
        "sand.threads.running",
        LocalizeValue.localizeTODO("Running threads"),
        ChartUnit.COUNT,
        "threads",
        TimeSeriesKind.LINE
    );

    private static final Logger LOG = Logger.getInstance(SandProfilerMonitor.class);

    private static final long PERIOD_MS = 500;
    private static final double MB = 1024 * 1024;
    private static final double INITIAL_HEAP_USED = 48 * MB;
    private static final double INITIAL_HEAP_COMMITTED = 128 * MB;
    private static final double MAX_HEAP_COMMITTED = 1024 * MB;
    private static final double HEAP_COMMIT_STEP = 64 * MB;
    private static final double STATE_CHANGE_PROBABILITY = 0.3;

    private final Project myProject;
    private final ProfilerMonitorSink mySink;
    private final List<NativeThread> myThreads;
    private final Consumer<SandProfilerMonitor> myOnDispose;
    private final Random myRandom = new Random();
    private final Object myLock = new Object();
    private final ProfilerThreadState[] myThreadStates;

    private volatile boolean myDisposed;
    private volatile @Nullable ScheduledFuture<?> myFuture;

    private boolean myTerminated;
    private long myTick;
    private double myHeapUsed = INITIAL_HEAP_USED;
    private double myHeapCommitted = INITIAL_HEAP_COMMITTED;

    SandProfilerMonitor(Project project, ProfilerMonitorSink sink, List<NativeThread> threads, Consumer<SandProfilerMonitor> onDispose) {
        myProject = project;
        mySink = sink;
        myThreads = threads;
        myOnDispose = onDispose;
        myThreadStates = new ProfilerThreadState[threads.size()];
        Arrays.fill(myThreadStates, ProfilerThreadState.RUNNING);
    }

    void start(ScheduledExecutorService scheduler) {
        myFuture = scheduler.scheduleWithFixedDelay(this, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        if (myDisposed) {
            cancel();
        }
    }

    void terminate(Instant time) {
        synchronized (myLock) {
            if (!myTerminated) {
                myTerminated = true;
                if (!myDisposed && myTick > 0) {
                    try {
                        for (int i = 0; i < myThreads.size(); i++) {
                            NativeThread thread = myThreads.get(i);
                            myThreadStates[i] = ProfilerThreadState.TERMINATED;
                            mySink.threadState(thread.getId(), thread.getName(), time, ProfilerThreadState.TERMINATED);
                        }
                    }
                    catch (RuntimeException e) {
                        LOG.error("Failed to report the end of the sand profiler threads", e);
                    }
                }
            }
        }
        cancel();
    }

    @Override
    public void run() {
        synchronized (myLock) {
            if (myDisposed || myTerminated) {
                return;
            }
            if (myProject.isDisposed()) {
                myTerminated = true;
                cancel();
                return;
            }

            try {
                tick(Instant.now());
            }
            catch (RuntimeException e) {
                myTerminated = true;
                cancel();
                LOG.error("Sand profiler monitor stopped after a failure", e);
            }
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
        cancel();
        myOnDispose.accept(this);
    }

    private void cancel() {
        ScheduledFuture<?> future = myFuture;
        if (future != null) {
            future.cancel(false);
        }
    }

    private void tick(Instant time) {
        int runningThreads = 0;
        for (int i = 0; i < myThreads.size(); i++) {
            ProfilerThreadState previous = myThreadStates[i];
            ProfilerThreadState next = myTick == 0 || myRandom.nextDouble() < STATE_CHANGE_PROBABILITY ? nextThreadState(i) : previous;
            myThreadStates[i] = next;
            if (myTick == 0 || next != previous) {
                NativeThread thread = myThreads.get(i);
                mySink.threadState(thread.getId(), thread.getName(), time, next);
            }
            if (next == ProfilerThreadState.RUNNING || next == ProfilerThreadState.RUNNING_NATIVE) {
                runningThreads++;
            }
        }

        double load = (double) runningThreads / myThreads.size();
        double processCpu = Math.clamp(load * 70 + 15 * Math.sin(myTick / 6.0) + myRandom.nextGaussian() * 4, 0, 100);
        double systemCpu = Math.clamp(processCpu + 10 + myRandom.nextGaussian() * 3, processCpu, 100);

        myHeapUsed += (1 + myRandom.nextDouble() * 5) * MB;
        if (myHeapUsed > myHeapCommitted * 0.85) {
            if (myHeapCommitted < MAX_HEAP_COMMITTED && myRandom.nextDouble() < 0.3) {
                myHeapCommitted = Math.min(MAX_HEAP_COMMITTED, myHeapCommitted + HEAP_COMMIT_STEP);
            }
            else {
                myHeapUsed = myHeapCommitted * (0.25 + myRandom.nextDouble() * 0.15);
            }
        }

        mySink.metric(PROCESS_CPU, time, processCpu);
        mySink.metric(SYSTEM_CPU, time, systemCpu);
        mySink.metric(HEAP_USED, time, myHeapUsed);
        mySink.metric(HEAP_COMMITTED, time, myHeapCommitted);
        mySink.metric(RUNNING_THREADS, time, runningThreads);

        myTick++;
    }

    private ProfilerThreadState nextThreadState(int threadIndex) {
        double roll = myRandom.nextDouble();
        if (threadIndex == 0) {
            if (roll < 0.7) {
                return ProfilerThreadState.RUNNING;
            }
            if (roll < 0.8) {
                return ProfilerThreadState.RUNNING_NATIVE;
            }
            if (roll < 0.9) {
                return ProfilerThreadState.WAITING;
            }
            return ProfilerThreadState.BLOCKED;
        }

        if (roll < 0.35) {
            return ProfilerThreadState.RUNNING;
        }
        if (roll < 0.45) {
            return ProfilerThreadState.RUNNING_NATIVE;
        }
        if (roll < 0.7) {
            return ProfilerThreadState.WAITING;
        }
        if (roll < 0.9) {
            return ProfilerThreadState.SLEEPING;
        }
        return ProfilerThreadState.BLOCKED;
    }
}
