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
package consulo.desktop.qt.ui.impl.image;

import consulo.logging.Logger;
import consulo.ui.UIAccess;
import io.qt.core.QRect;
import io.qt.gui.QRegion;
import io.qt.widgets.QAbstractScrollArea;
import io.qt.widgets.QScrollBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class DesktopQtAnimationTicker {
    private static final Logger LOG = Logger.getInstance(DesktopQtAnimationTicker.class);

    private static final long NANOS_IN_MILLI = TimeUnit.MILLISECONDS.toNanos(1);
    private static final long TOLERANCE = 2 * NANOS_IN_MILLI;
    private static final long LOST_TASK = 1000 * NANOS_IN_MILLI;

    private static final class WidgetRequest {
        private final @Nullable QAbstractScrollArea myScrollArea;
        private final int myHorizontalValue;
        private final int myVerticalValue;
        private final Map<QRect, Long> myAreas = new HashMap<>();

        private WidgetRequest(@Nullable QAbstractScrollArea scrollArea) {
            myScrollArea = scrollArea;
            myHorizontalValue = scrollArea == null ? 0 : value(scrollArea.horizontalScrollBar());
            myVerticalValue = scrollArea == null ? 0 : value(scrollArea.verticalScrollBar());
        }

        private static WidgetRequest create(QWidget widget) {
            if (widget.parentWidget() instanceof QAbstractScrollArea scrollArea && scrollArea.viewport() == widget) {
                return new WidgetRequest(scrollArea);
            }
            return new WidgetRequest(null);
        }

        private boolean isScrolled() {
            QAbstractScrollArea scrollArea = myScrollArea;
            if (scrollArea == null || scrollArea.isDisposed()) {
                return false;
            }

            return value(scrollArea.horizontalScrollBar()) != myHorizontalValue || value(scrollArea.verticalScrollBar()) != myVerticalValue;
        }

        private static int value(@Nullable QScrollBar scrollBar) {
            return scrollBar == null || scrollBar.isDisposed() ? 0 : scrollBar.value();
        }
    }

    private static final Map<QWidget, WidgetRequest> ourWidgets = new LinkedHashMap<>();
    private static final Map<DesktopQtAnimationHost, Long> ourHosts = new IdentityHashMap<>();

    private static @Nullable ScheduledFuture<?> ourTask;
    private static long ourTaskDeadline;
    private static long ourGeneration;

    private DesktopQtAnimationTicker() {
    }

    public static void request(QWidget widget, QRect rect, long delayMillis) {
        if (delayMillis <= 0 || rect.isEmpty() || !UIAccess.isUIThread() || widget.isDisposed()) {
            return;
        }

        long deadline = System.nanoTime() + delayMillis * NANOS_IN_MILLI;

        WidgetRequest request = ourWidgets.computeIfAbsent(widget, WidgetRequest::create);
        request.myAreas.merge(rect, deadline, Math::min);

        schedule(deadline);
    }

    public static void request(DesktopQtAnimationHost host, long delayMillis) {
        if (delayMillis <= 0 || !UIAccess.isUIThread()) {
            return;
        }

        long deadline = System.nanoTime() + delayMillis * NANOS_IN_MILLI;

        ourHosts.merge(host, deadline, Math::min);

        schedule(deadline);
    }

    private static void schedule(long deadline) {
        ScheduledFuture<?> task = ourTask;
        if (task != null) {
            if (ourTaskDeadline <= deadline + TOLERANCE && System.nanoTime() - ourTaskDeadline < LOST_TASK) {
                return;
            }

            task.cancel(false);
            ourTask = null;
        }

        long generation = ++ourGeneration;
        long delay = Math.max(1, Math.ceilDiv(deadline - System.nanoTime(), NANOS_IN_MILLI));

        try {
            ourTask = UIAccess.current().getScheduler().schedule(() -> fire(generation), delay, TimeUnit.MILLISECONDS);
            ourTaskDeadline = deadline;
        }
        catch (Throwable e) {
            ourTask = null;
            LOG.error(e);
        }
    }

    private static void fire(long generation) {
        if (generation != ourGeneration) {
            return;
        }

        ourTask = null;

        long limit = System.nanoTime() + TOLERANCE;
        long next = Long.MAX_VALUE;

        List<QWidget> wholeWidgets = new ArrayList<>();
        Map<QWidget, QRegion> regions = new LinkedHashMap<>();
        List<DesktopQtAnimationHost> hosts = new ArrayList<>();

        Iterator<Map.Entry<QWidget, WidgetRequest>> widgetIterator = ourWidgets.entrySet().iterator();
        while (widgetIterator.hasNext()) {
            Map.Entry<QWidget, WidgetRequest> entry = widgetIterator.next();

            QWidget widget = entry.getKey();
            WidgetRequest request = entry.getValue();

            if (widget.isDisposed()) {
                widgetIterator.remove();
                continue;
            }

            if (request.isScrolled()) {
                wholeWidgets.add(widget);
                widgetIterator.remove();
                continue;
            }

            QRegion region = null;

            Iterator<Map.Entry<QRect, Long>> areaIterator = request.myAreas.entrySet().iterator();
            while (areaIterator.hasNext()) {
                Map.Entry<QRect, Long> area = areaIterator.next();

                long deadline = area.getValue();
                if (deadline <= limit) {
                    region = region == null ? new QRegion(area.getKey()) : region.united(area.getKey());
                    areaIterator.remove();
                }
                else {
                    next = Math.min(next, deadline);
                }
            }

            if (region != null) {
                regions.put(widget, region);
            }

            if (request.myAreas.isEmpty()) {
                widgetIterator.remove();
            }
        }

        Iterator<Map.Entry<DesktopQtAnimationHost, Long>> hostIterator = ourHosts.entrySet().iterator();
        while (hostIterator.hasNext()) {
            Map.Entry<DesktopQtAnimationHost, Long> entry = hostIterator.next();

            long deadline = entry.getValue();
            if (deadline <= limit) {
                hosts.add(entry.getKey());
                hostIterator.remove();
            }
            else {
                next = Math.min(next, deadline);
            }
        }

        if (next != Long.MAX_VALUE) {
            schedule(next);
        }

        for (QWidget widget : wholeWidgets) {
            if (!widget.isDisposed()) {
                widget.update();
            }
        }

        for (Map.Entry<QWidget, QRegion> entry : regions.entrySet()) {
            QWidget widget = entry.getKey();
            if (!widget.isDisposed()) {
                widget.update(entry.getValue());
            }
        }

        for (DesktopQtAnimationHost host : hosts) {
            try {
                host.repaintAnimatedImage();
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }
}
