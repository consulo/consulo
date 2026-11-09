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
package consulo.continuousIntegration.impl.internal.monitor;

import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.CICapability;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CILogChunk;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.internal.CIUtil;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.BaseProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Remote build as a process: waits for the build to start, polls its state, streams its log by offset and stops it on
 * {@link #destroyProcess()}. The process terminates once the build is finished and its log is read.
 * <p>
 * Polling is one chain of steps: each step schedules the next one only after its request completed, so the chain
 * state below is never touched by two threads at once.
 *
 * @since 2026-11-09
 */
public final class CIBuildProcessHandler extends BaseProcessHandler {
    private static final long POLL_DELAY_MS = 3000;
    private static final Logger LOG = Logger.getInstance(CIBuildProcessHandler.class);

    private static final long LOG_RETRY_DELAY_MS = 1000;
    private static final long STOP_TIMEOUT_MS = 30_000;
    private static final int MAX_FAILURES = 5;
    private static final int MAX_EMPTY_FINAL_CHUNKS = 5;

    private final CIBuildMonitorImpl myMonitor;
    private final CIServer myServer;
    private final CIJob myJob;
    private final CompletableFuture<CIBuild> myStart;
    private final @Nullable CIBuild myAttachedBuild;
    private final ScheduledExecutorService myScheduler;

    private final AtomicBoolean myFinished = new AtomicBoolean();
    private final AtomicBoolean myStopSent = new AtomicBoolean();
    private volatile @Nullable CIBuild myBuild;
    private volatile boolean myStopRequested;
    private volatile long myStopDeadline;
    private volatile boolean mySeenActive;
    private volatile @Nullable ScheduledFuture<?> myScheduled;

    // state of the polling chain
    private final AnsiEscapeDecoder myAnsiDecoder = new AnsiEscapeDecoder();
    private @Nullable CIBuildStatus myLastStatus;
    private long myLogOffset;
    private boolean myLogComplete;
    private int myFailures;
    private int myEmptyFinalChunks;

    CIBuildProcessHandler(
        CIBuildMonitorImpl monitor,
        CIServer server,
        CIJob job,
        CompletableFuture<CIBuild> start,
        @Nullable CIBuild attachedBuild,
        ScheduledExecutorService scheduler
    ) {
        myMonitor = monitor;
        myServer = server;
        myJob = job;
        myStart = start;
        myAttachedBuild = attachedBuild;
        myScheduler = scheduler;
    }

    public CIServer getServer() {
        return myServer;
    }

    public CIJob getJob() {
        return myJob;
    }

    public @Nullable CIBuild getBuild() {
        return myBuild;
    }

    public boolean canStop() {
        return !isProcessTerminating() && !isProcessTerminated() && myServer.hasCapability(CICapability.STOP);
    }

    @Override
    public void startNotify() {
        super.startNotify();

        CIBuild attachedBuild = myAttachedBuild;
        if (attachedBuild != null) {
            system(ContinuousIntegrationImplLocalize.monitorFollowing(myJob.displayName(), attachedBuild.displayName()));
        }
        else {
            system(ContinuousIntegrationImplLocalize.monitorStarting(myJob.displayName()));
        }

        myStart.whenComplete((build, error) -> {
            if (error != null || build == null) {
                stderr(ContinuousIntegrationImplLocalize.monitorStartFailed(error == null ? "" : CIUtil.errorMessage(error)));
                finish(null);
                return;
            }
            try {
                started(build);
            }
            catch (RuntimeException e) {
                LOG.error(e);
                stderr(ContinuousIntegrationImplLocalize.monitorStartFailed(CIUtil.errorMessage(e)));
                finish(build);
            }
        });
    }

    private void started(CIBuild build) {
        myBuild = build;
        myMonitor.buildStarted(this, build);

        String webUrl = build.webUrl();
        if (webUrl != null) {
            system(ContinuousIntegrationImplLocalize.monitorBuildPage(webUrl));
        }

        if (myStopRequested) {
            requestStop(build);
        }
        poll();
    }

    private void poll() {
        CIBuild build = myBuild;
        if (myFinished.get() || build == null) {
            return;
        }

        call(() -> myServer.loadBuild(build)).whenComplete((updated, error) -> {
            if (myFinished.get()) {
                return;
            }
            if (error != null || updated == null) {
                failed(ContinuousIntegrationImplLocalize.monitorPollFailed(error == null ? "" : CIUtil.errorMessage(error)), this::poll);
                return;
            }

            myFailures = 0;
            updated(updated);
            readLog(updated);
        });
    }

    private void updated(CIBuild build) {
        myBuild = build;

        CIBuildStatus status = build.status();
        if (status.isActive()) {
            mySeenActive = true;
        }
        if (status != myLastStatus) {
            myLastStatus = status;
            system(ContinuousIntegrationImplLocalize.monitorStatus(myJob.displayName(), build.displayName(), status.getPresentableName()));
            myMonitor.buildChanged(this, build);
        }
    }

    private void readLog(CIBuild build) {
        boolean finished = build.status().isFinished();
        boolean canRead = myServer.hasCapability(CICapability.LOG)
            && !myLogComplete
            && build.status() != CIBuildStatus.QUEUED
            && (finished || myServer.hasCapability(CICapability.PROGRESSIVE_LOG));
        if (!canRead) {
            if (finished) {
                finish(build);
            }
            else {
                schedule(this::poll, POLL_DELAY_MS);
            }
            return;
        }

        call(() -> myServer.loadLog(build, null, myLogOffset)).whenComplete((chunk, error) -> {
            if (myFinished.get()) {
                return;
            }
            if (error != null || chunk == null) {
                LocalizeValue message = ContinuousIntegrationImplLocalize.monitorLogFailed(error == null ? "" : CIUtil.errorMessage(error));
                if (finished) {
                    stderr(message);
                    finish(build);
                }
                else {
                    failed(message, this::poll);
                }
                return;
            }

            logRead(build, chunk, finished);
        });
    }

    private void logRead(CIBuild build, CILogChunk chunk, boolean finished) {
        String text = chunk.text();
        if (!text.isEmpty()) {
            myAnsiDecoder.escapeText(text, ProcessOutputTypes.STDOUT, this::notifyTextAvailable);
        }
        myLogOffset = chunk.nextOffset();
        myLogComplete = chunk.complete();

        if (!finished) {
            schedule(this::poll, POLL_DELAY_MS);
        }
        else if (myLogComplete || (text.isEmpty() && ++myEmptyFinalChunks >= MAX_EMPTY_FINAL_CHUNKS)) {
            finish(build);
        }
        else {
            schedule(() -> readLog(build), text.isEmpty() ? LOG_RETRY_DELAY_MS : 0);
        }
    }

    private void failed(LocalizeValue message, Runnable retry) {
        stderr(message);
        myFailures++;
        if (myStopRequested) {
            // a stopped queue item or build may be gone already, there is nothing left to follow
            finish(myBuild);
        }
        else if (myFailures >= MAX_FAILURES) {
            stderr(ContinuousIntegrationImplLocalize.monitorPollGaveUp());
            finish(myBuild);
        }
        else {
            schedule(retry, POLL_DELAY_MS);
        }
    }

    private void schedule(Runnable task, long delayMs) {
        if (myFinished.get()) {
            return;
        }
        if (myStopRequested && System.currentTimeMillis() > myStopDeadline) {
            finish(myBuild);
            return;
        }
        try {
            myScheduled = myScheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException e) {
            finish(myBuild);
        }
    }

    @Override
    protected void destroyProcessImpl() {
        myStopDeadline = System.currentTimeMillis() + STOP_TIMEOUT_MS;
        myStopRequested = true;

        CIBuild build = myBuild;
        if (build == null) {
            system(ContinuousIntegrationImplLocalize.monitorStopPending());
            return;
        }
        requestStop(build);
    }

    private void requestStop(CIBuild build) {
        if (!myStopSent.compareAndSet(false, true)) {
            return;
        }

        system(ContinuousIntegrationImplLocalize.monitorStopping(build.displayName()));
        if (!myServer.hasCapability(CICapability.STOP)) {
            stderr(ContinuousIntegrationImplLocalize.monitorStopUnsupported());
            finish(myBuild);
            return;
        }

        call(() -> myServer.stopBuild(build)).whenComplete((result, error) -> {
            if (error != null) {
                stderr(ContinuousIntegrationImplLocalize.monitorStopFailed(CIUtil.errorMessage(error)));
                finish(myBuild);
            }
            else {
                // keep polling: the build reports its final state and the rest of its log
                system(ContinuousIntegrationImplLocalize.monitorStopRequested());
            }
        });
    }

    private void finish(@Nullable CIBuild build) {
        if (!myFinished.compareAndSet(false, true)) {
            return;
        }
        cancelScheduled();

        int exitCode = 1;
        boolean notify = false;
        if (build != null && build.status().isFinished()) {
            LocalizeValue status = build.status().getPresentableName();
            system(ContinuousIntegrationImplLocalize.monitorFinished(myJob.displayName(), build.displayName(), status));
            exitCode = build.status().isSuccessful() ? 0 : 1;
            notify = mySeenActive && !myStopRequested;
        }
        myMonitor.buildFinished(this, build, notify);
        notifyProcessTerminated(exitCode);
    }

    @Override
    protected void detachProcessImpl() {
        if (myFinished.compareAndSet(false, true)) {
            cancelScheduled();
            myMonitor.buildFinished(this, myBuild, false);
        }
        notifyProcessDetached();
    }

    private void cancelScheduled() {
        ScheduledFuture<?> scheduled = myScheduled;
        if (scheduled != null) {
            scheduled.cancel(false);
        }
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return null;
    }

    private void system(LocalizeValue text) {
        print(text, ProcessOutputTypes.SYSTEM);
    }

    private void stderr(LocalizeValue text) {
        print(text, ProcessOutputTypes.STDERR);
    }

    private void print(LocalizeValue text, Key outputType) {
        notifyTextAvailable(text.get() + "\n", outputType);
    }

    /**
     * A provider which throws instead of returning a failed future must not break the polling chain.
     */
    private static <T> CompletableFuture<T> call(Supplier<CompletableFuture<T>> request) {
        try {
            return request.get();
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
