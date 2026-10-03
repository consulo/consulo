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

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.execution.profiler.CallTreeBuilder;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.CollapsedProfilerDumpWriter;
import consulo.execution.profiler.DataReady;
import consulo.execution.profiler.NewCallTreeOnlyProfilerData;
import consulo.execution.profiler.ProfilerData;
import consulo.execution.profiler.ProfilingFailed;
import consulo.execution.profiler.configuration.ProfilerFeature;
import consulo.execution.profiler.live.LiveProfilerProcess;
import consulo.execution.profiler.live.ProfilerMonitorSink;
import consulo.execution.profiler.model.NativeThread;
import consulo.execution.profiler.model.ThreadInfo;
import consulo.execution.profiler.ui.NativeCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class SandProfilerProcess extends ProfilerProcess<SandTargetProcess> implements LiveProfilerProcess {
    public static final Set<ProfilerFeature> FEATURES = Set.of(ProfilerFeature.LIVE_MONITORING, ProfilerFeature.CPU_RECORDING);

    private static final Logger LOG = Logger.getInstance(SandProfilerProcess.class);

    private static final long NO_RECORDING = -1;

    private final ApplicationConcurrency myConcurrency;
    private final SandProfilerConfigurationState myConfiguration;
    private final @Nullable ProcessHandler myProcessHandler;
    private final List<NativeThread> myThreads;
    private final int mySamplingIntervalMs;
    private final long myAttachedTimestamp = System.currentTimeMillis();
    private final List<SandProfilerMonitor> myMonitors = new CopyOnWriteArrayList<>();
    private final AtomicBoolean myFinished = new AtomicBoolean();
    private final AtomicLong myRecordingStart = new AtomicLong(NO_RECORDING);

    private SandProfilerProcess(
        Project project,
        ApplicationConcurrency concurrency,
        SandTargetProcess targetProcess,
        SandProfilerConfigurationState configuration,
        @Nullable ProcessHandler processHandler
    ) {
        super(project, targetProcess);
        myConcurrency = concurrency;
        myConfiguration = configuration;
        myProcessHandler = processHandler;
        myThreads = SandCallTreeGenerator.createThreads(configuration.getSyntheticThreadCount());
        mySamplingIntervalMs = configuration.getSamplingIntervalMs();
    }

    public static SandProfilerProcess launched(
        Project project,
        ApplicationConcurrency concurrency,
        SandTargetProcess targetProcess,
        SandProfilerConfigurationState configuration,
        ProcessHandler processHandler
    ) {
        SandProfilerProcess process = new SandProfilerProcess(project, concurrency, targetProcess, configuration, processHandler);
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void processTerminated(ProcessEvent event) {
                process.finish();
            }
        });
        if (processHandler.isProcessTerminated()) {
            process.finish();
        }
        return process;
    }

    public static SandProfilerProcess attached(
        Project project,
        ApplicationConcurrency concurrency,
        SandTargetProcess targetProcess,
        SandProfilerConfigurationState configuration
    ) {
        return new SandProfilerProcess(project, concurrency, targetProcess, configuration, null);
    }

    @Override
    public long getAttachedTimestamp() {
        return myAttachedTimestamp;
    }

    @Override
    public SandProfilerConfigurationState getProfilerConfiguration() {
        return myConfiguration;
    }

    @Override
    public boolean canStop() {
        return !myFinished.get();
    }

    @Override
    public void stop() {
        ProcessHandler processHandler = myProcessHandler;
        if (processHandler != null && !processHandler.isProcessTerminated()) {
            if (!processHandler.isProcessTerminating()) {
                processHandler.destroyProcess();
            }
            return;
        }
        finish();
    }

    @Override
    public Set<ProfilerFeature> getFeatures() {
        return FEATURES;
    }

    @Override
    public Disposable startMonitoring(ProfilerMonitorSink sink) {
        if (myFinished.get()) {
            return () -> {
            };
        }

        SandProfilerMonitor monitor = new SandProfilerMonitor(getProject(), sink, myThreads, myMonitors::remove);
        myMonitors.add(monitor);
        monitor.start(myConcurrency.getScheduledExecutorService());
        if (myFinished.get()) {
            myConcurrency.executor().execute(() -> monitor.terminate(Instant.now()));
        }
        return monitor;
    }

    @Override
    public CompletableFuture<?> startCpuRecording() {
        if (myFinished.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Profiling of " + getTargetProcess().getFullName() + " has already ended")
            );
        }
        myRecordingStart.compareAndSet(NO_RECORDING, System.currentTimeMillis());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ProfilerData> stopCpuRecording() {
        long stopped = System.currentTimeMillis();
        long started = myRecordingStart.getAndSet(NO_RECORDING);
        long from = started == NO_RECORDING ? myAttachedTimestamp : started;
        return CompletableFuture.<ProfilerData>supplyAsync(
            () -> new NewCallTreeOnlyProfilerData(generate(from, stopped), NativeCallStackElementRenderer.INSTANCE),
            myConcurrency.executor()
        );
    }

    private void finish() {
        if (!myFinished.compareAndSet(false, true)) {
            return;
        }

        long finished = System.currentTimeMillis();
        myConcurrency.executor().execute(() -> {
            Instant finishedTime = Instant.ofEpochMilli(finished);
            for (SandProfilerMonitor monitor : myMonitors) {
                monitor.terminate(finishedTime);
            }
            myMonitors.clear();

            if (getProject().isDisposed()) {
                return;
            }

            try {
                CallTreeBuilder<BaseCallStackElement> builder = generate(myAttachedTimestamp, finished);
                CollapsedProfilerDumpWriter dumpWriter = new CollapsedProfilerDumpWriter(
                    builder,
                    getTargetProcess().getFullName(),
                    myAttachedTimestamp,
                    BaseCallStackElement::fullName,
                    ThreadInfo::getName
                );
                setState(new DataReady(new NewCallTreeOnlyProfilerData(builder, NativeCallStackElementRenderer.INSTANCE), dumpWriter));
            }
            catch (RuntimeException e) {
                LOG.error("Failed to build the sand profile of " + getTargetProcess().getFullName(), e);
                setState(new ProfilingFailed(LocalizeValue.localizeTODO("Failed to build the sand profile")));
            }
        });
    }

    private CallTreeBuilder<BaseCallStackElement> generate(long from, long to) {
        return SandCallTreeGenerator.generate(myThreads, Math.max(0, to - from), mySamplingIntervalMs, new Random());
    }
}
