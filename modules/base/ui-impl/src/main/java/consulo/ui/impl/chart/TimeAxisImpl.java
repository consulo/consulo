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
package consulo.ui.impl.chart;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.TimeAxis;
import consulo.ui.chart.TimeRange;
import consulo.ui.impl.chart.model.AspectObserver;
import consulo.ui.impl.chart.model.Range;
import consulo.ui.impl.chart.model.StopwatchTimer;
import consulo.ui.impl.chart.model.StreamingTimeline;
import consulo.ui.impl.chart.model.axis.ResizingAxisComponentModel;
import consulo.ui.impl.chart.model.formatter.TimeAxisFormatter;
import consulo.ui.impl.chart.model.updater.Updater;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class TimeAxisImpl implements TimeAxis {
    private final Updater myUpdater;
    private final StreamingTimeline myTimeline;
    private final Executor myModelExecutor;

    private int myUsers;
    private long myStoppedAtNs;

    public TimeAxisImpl(StopwatchTimer timer, Instant dataStart) {
        this(timer, AppExecutorUtil.getAppExecutorService(), dataStart);
    }

    public TimeAxisImpl(StopwatchTimer timer, Executor modelExecutor, Instant dataStart) {
        myModelExecutor = modelExecutor;
        myUpdater = new Updater(timer);
        myTimeline = new StreamingTimeline(myUpdater);
        long nowNs = ChartTime.toNanos(Instant.now());
        myTimeline.reset(Math.min(ChartTime.toNanos(dataStart), nowNs), nowNs);
        myUpdater.stop();
        myStoppedAtNs = timer.getCurrentTimeNs();
    }

    public Updater getUpdater() {
        return myUpdater;
    }

    public StreamingTimeline getTimeline() {
        return myTimeline;
    }

    public Executor getModelExecutor() {
        return myModelExecutor;
    }

    public ResizingAxisComponentModel createTimeAxisModel() {
        return new ResizingAxisComponentModel.Builder(myTimeline.getViewRange(), TimeAxisFormatter.DEFAULT)
            .setGlobalRange(myTimeline.getDataRange())
            .build();
    }

    public void retain() {
        if (myUsers++ == 0) {
            StopwatchTimer timer = myUpdater.getTimer();
            long pausedNs = timer.getCurrentTimeNs() - myStoppedAtNs;
            if (pausedNs > 0) {
                myUpdater.onTick(pausedNs);
            }
            timer.start();
        }
    }

    public void release() {
        if (myUsers > 0 && --myUsers == 0) {
            myUpdater.stop();
            myStoppedAtNs = myUpdater.getTimer().getCurrentTimeNs();
        }
    }

    @Override
    public @Nullable TimeRange getVisible() {
        return toTimeRange(myTimeline.getViewRange());
    }

    @RequiredUIAccess
    @Override
    public void setVisible(TimeRange range) {
        myTimeline.setStreaming(false);
        myTimeline.getViewRange().set(ChartTime.toMicros(range.from()), ChartTime.toMicros(range.to()));
    }

    @RequiredUIAccess
    @Override
    public void setFollowLatest(Duration window) {
        double max = myTimeline.getDataRange().getMax();
        myTimeline.getViewRange().set(max - window.toNanos() / 1_000d, max);
        myTimeline.setStreaming(true);
    }

    @Override
    public boolean isFollowingLatest() {
        return myTimeline.isStreaming();
    }

    @Override
    public @Nullable TimeRange getSelection() {
        return toTimeRange(myTimeline.getSelectionRange());
    }

    @RequiredUIAccess
    @Override
    public void setSelection(@Nullable TimeRange selection) {
        if (selection == null) {
            myTimeline.getSelectionRange().clear();
        }
        else {
            myTimeline.getSelectionRange().set(ChartTime.toMicros(selection.from()), ChartTime.toMicros(selection.to()));
        }
    }

    @Override
    public Disposable addVisibleListener(Consumer<TimeRange> listener) {
        Range range = myTimeline.getViewRange();
        return listen(range, () -> {
            TimeRange visible = toTimeRange(range);
            if (visible != null) {
                listener.accept(visible);
            }
        });
    }

    @Override
    public Disposable addSelectionListener(Consumer<@Nullable TimeRange> listener) {
        Range range = myTimeline.getSelectionRange();
        return listen(range, () -> listener.accept(toTimeRange(range)));
    }

    private static Disposable listen(Range range, Runnable runnable) {
        AspectObserver observer = new AspectObserver();
        range.addDependency(observer).onChange(Range.Aspect.RANGE, runnable);
        return () -> range.removeDependencies(observer);
    }

    private static @Nullable TimeRange toTimeRange(Range range) {
        if (range.isEmpty()) {
            return null;
        }
        return new TimeRange(ChartTime.fromMicros(range.getMin()), ChartTime.fromMicros(range.getMax()));
    }
}
