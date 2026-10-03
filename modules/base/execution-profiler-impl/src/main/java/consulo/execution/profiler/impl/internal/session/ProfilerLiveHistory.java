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
package consulo.execution.profiler.impl.internal.session;

import consulo.disposer.Disposable;
import consulo.execution.profiler.live.ProfilerMetric;
import consulo.execution.profiler.live.ProfilerMonitorSink;
import consulo.execution.profiler.live.ProfilerThreadState;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerLiveHistory implements ProfilerMonitorSink {
    private static final Logger LOG = Logger.getInstance(ProfilerLiveHistory.class);

    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;

    private static final int MAX_THREADS = 500;

    private final Object myLock = new Object();
    private final Map<String, ProfilerMetricHistory> myMetrics = new LinkedHashMap<>();
    private final Map<Long, ProfilerThreadHistory> myThreads = new LinkedHashMap<>();
    private final List<ProfilerMonitorSink> mySinks = new ArrayList<>();

    private boolean myClosed;

    @Override
    public void metric(ProfilerMetric metric, Instant time, double value) {
        synchronized (myLock) {
            if (myClosed) {
                return;
            }

            ProfilerMetricHistory history = myMetrics.get(metric.id());
            if (history == null) {
                history = new ProfilerMetricHistory(metric);
                myMetrics.put(metric.id(), history);
            }
            if (history.add(toMicros(time), value)) {
                pruneTerminatedThreads();
            }

            ProfilerMetric descriptor = history.getMetric();
            forEachSink(sink -> sink.metric(descriptor, time, value));
        }
    }

    @Override
    public void threadState(long threadId, String threadName, Instant time, ProfilerThreadState state) {
        synchronized (myLock) {
            if (myClosed) {
                return;
            }

            ProfilerThreadHistory history = myThreads.get(threadId);
            if (history == null) {
                if (myThreads.size() >= MAX_THREADS) {
                    evictThread();
                }
                history = new ProfilerThreadHistory(threadId, threadName);
                myThreads.put(threadId, history);
            }

            if (history.set(threadName, toMicros(time), state)) {
                forEachSink(sink -> sink.threadState(threadId, threadName, time, state));
            }
        }
    }

    public Disposable attach(ProfilerMonitorSink sink) {
        synchronized (myLock) {
            if (myClosed) {
                return () -> {
                };
            }

            try {
                long windowStart = getWindowStart();
                for (ProfilerMetricHistory history : myMetrics.values()) {
                    history.replay(sink);
                }
                for (ProfilerThreadHistory history : myThreads.values()) {
                    if (!history.isTerminatedBefore(windowStart)) {
                        history.replay(sink, windowStart);
                    }
                }
            }
            catch (Throwable e) {
                LOG.error("Failed to replay the live profiler history into " + sink, e);
                return () -> {
                };
            }

            mySinks.add(sink);
        }

        return () -> {
            synchronized (myLock) {
                mySinks.remove(sink);
            }
        };
    }

    public @Nullable Instant getFirstTime() {
        synchronized (myLock) {
            if (!myMetrics.isEmpty()) {
                return toInstant(getWindowStart());
            }

            long first = Long.MAX_VALUE;
            for (ProfilerThreadHistory history : myThreads.values()) {
                first = Math.min(first, history.getFirstTime());
            }
            return first == Long.MAX_VALUE ? null : toInstant(first);
        }
    }

    public @Nullable Instant getLastTime() {
        synchronized (myLock) {
            long last = Long.MIN_VALUE;
            for (ProfilerMetricHistory history : myMetrics.values()) {
                last = Math.max(last, history.getLastTime());
            }
            for (ProfilerThreadHistory history : myThreads.values()) {
                last = Math.max(last, history.getLastTime());
            }
            return last == Long.MIN_VALUE ? null : toInstant(last);
        }
    }

    public void close() {
        synchronized (myLock) {
            myClosed = true;
            myMetrics.clear();
            myThreads.clear();
            mySinks.clear();
        }
    }

    static long toMicros(Instant time) {
        return Math.addExact(Math.multiplyExact(time.getEpochSecond(), MICROS_PER_SECOND), time.getNano() / NANOS_PER_MICRO);
    }

    static Instant toInstant(long micros) {
        return Instant.ofEpochSecond(
            Math.floorDiv(micros, MICROS_PER_SECOND),
            Math.floorMod(micros, MICROS_PER_SECOND) * NANOS_PER_MICRO
        );
    }

    private long getWindowStart() {
        long start = Long.MAX_VALUE;
        for (ProfilerMetricHistory history : myMetrics.values()) {
            start = Math.min(start, history.getFirstTime());
        }
        return start == Long.MAX_VALUE ? Long.MIN_VALUE : start;
    }

    private void pruneTerminatedThreads() {
        long windowStart = getWindowStart();
        myThreads.values().removeIf(history -> history.isTerminatedBefore(windowStart));
    }

    private void evictThread() {
        ProfilerThreadHistory leastRecent = null;
        Iterator<ProfilerThreadHistory> iterator = myThreads.values().iterator();
        while (iterator.hasNext()) {
            ProfilerThreadHistory history = iterator.next();
            if (history.isTerminated()) {
                iterator.remove();
                return;
            }
            if (leastRecent == null || history.getLastTime() < leastRecent.getLastTime()) {
                leastRecent = history;
            }
        }

        if (leastRecent != null) {
            myThreads.remove(leastRecent.getThreadId());
        }
    }

    private void forEachSink(Consumer<ProfilerMonitorSink> call) {
        Iterator<ProfilerMonitorSink> iterator = mySinks.iterator();
        while (iterator.hasNext()) {
            ProfilerMonitorSink sink = iterator.next();
            try {
                call.accept(sink);
            }
            catch (Throwable e) {
                iterator.remove();
                LOG.error("Live profiler sink " + sink + " failed and was detached", e);
            }
        }
    }
}
