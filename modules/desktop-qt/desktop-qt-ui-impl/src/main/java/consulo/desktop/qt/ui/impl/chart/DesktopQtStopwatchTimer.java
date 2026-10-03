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
package consulo.desktop.qt.ui.impl.chart;

import consulo.ui.impl.chart.model.StopwatchTimer;
import io.qt.core.QTimer;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class DesktopQtStopwatchTimer extends StopwatchTimer {
    private static final int FPS = 30;

    private @Nullable QTimer myTimer;
    private long myFrameTime;

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        QTimer timer = myTimer;
        if (timer == null) {
            timer = new QTimer();
            timer.setInterval(1000 / FPS);
            timer.timeout.connect(this::onTimeout);
            myTimer = timer;
        }
        myFrameTime = System.nanoTime();
        timer.start();
    }

    @Override
    public boolean isRunning() {
        QTimer timer = myTimer;
        return timer != null && timer.isActive();
    }

    @Override
    public void stop() {
        QTimer timer = myTimer;
        if (timer != null) {
            timer.stop();
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
