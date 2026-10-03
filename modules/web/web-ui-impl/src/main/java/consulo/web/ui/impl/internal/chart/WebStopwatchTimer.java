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
package consulo.web.ui.impl.internal.chart;

import consulo.ui.UIAccess;
import consulo.ui.impl.chart.model.StopwatchTimer;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class WebStopwatchTimer extends StopwatchTimer {
    private static final long INTERVAL_MS = 250;

    private final UIAccess myUIAccess;
    private @Nullable ScheduledFuture<?> myFuture;
    private long myFrameTime;

    public WebStopwatchTimer(UIAccess uiAccess) {
        myUIAccess = uiAccess;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        myFrameTime = System.nanoTime();
        myFuture = myUIAccess.getScheduler().scheduleWithFixedDelay(this::onTimeout, INTERVAL_MS, INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isRunning() {
        ScheduledFuture<?> future = myFuture;
        return future != null && !future.isDone();
    }

    @Override
    public void stop() {
        ScheduledFuture<?> future = myFuture;
        if (future != null) {
            future.cancel(false);
            myFuture = null;
        }
    }

    @Override
    public long getCurrentTimeNs() {
        return System.nanoTime();
    }

    private void onTimeout() {
        long now = System.nanoTime();
        long frame = now - myFrameTime;
        myFrameTime = now;
        tick(frame);
    }
}
