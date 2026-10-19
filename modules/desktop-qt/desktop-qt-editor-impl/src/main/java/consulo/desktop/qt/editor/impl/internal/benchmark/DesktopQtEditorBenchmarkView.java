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
package consulo.desktop.qt.editor.impl.internal.benchmark;

import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.internal.benchmark.EditorBenchmarkView;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorErrorStripeWidget;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorPainter;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorWidget;
import consulo.ide.impl.idea.codeInsight.hints.HintRenderer;
import io.qt.core.QEventLoop;
import io.qt.core.QLibraryInfo;
import io.qt.core.QRect;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QImage;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class DesktopQtEditorBenchmarkView implements EditorBenchmarkView {
    private final DesktopQtEditorImpl myEditor;
    private final DesktopQtEditorBenchmarkPaintWatcher myWatcher = new DesktopQtEditorBenchmarkPaintWatcher();
    private final QEventLoop myEventLoop = new QEventLoop();
    private final List<QTimer> myPausedTimers = new ArrayList<>();
    private @Nullable QImage myImage;
    private @Nullable DesktopQtEditorPainter myPainter;

    public DesktopQtEditorBenchmarkView(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    public String getFrontendName() {
        return "Qt " + QLibraryInfo.version() + " (" + QGuiApplication.platformName() + ")";
    }

    @Override
    public boolean isShown() {
        DesktopQtEditorWidget surface = myEditor.getSurface();
        return surface != null && surface.isVisible() && surface.viewport().height() > 0;
    }

    @Override
    public int getViewportWidth() {
        return surface().viewport().width();
    }

    @Override
    public int getViewportHeight() {
        return surface().viewport().height();
    }

    @Override
    public double getDevicePixelRatio() {
        return surface().devicePixelRatioF();
    }

    @Override
    public EditorCustomElementRenderer createHintRenderer(String text) {
        return new HintRenderer(text);
    }

    @Override
    public void beginRun() {
        DesktopQtEditorWidget surface = surface();
        for (QTimer timer : surface.findChildren(QTimer.class)) {
            if (timer.isActive()) {
                timer.stop();
                myPausedTimers.add(timer);
            }
        }
        surface.viewport().installEventFilter(myWatcher);
    }

    @Override
    public void endRun() {
        DesktopQtEditorWidget surface = surface();
        surface.viewport().removeEventFilter(myWatcher);
        myPausedTimers.forEach(QTimer::start);
        myPausedTimers.clear();

        if (myImage != null) {
            myImage.dispose();
            myImage = null;
        }
    }

    @Override
    public void paintTextOffscreen(int scrollY) {
        QWidget viewport = surface().viewport();
        QImage image = offscreenImage(viewport);
        DesktopQtEditorPainter painter = myPainter;
        if (painter == null) {
            painter = new DesktopQtEditorPainter(myEditor);
            myPainter = painter;
        }

        QPainter qPainter = new QPainter();
        qPainter.begin(image);
        try {
            painter.paint(qPainter, new QRect(0, 0, viewport.width(), viewport.height()), 0, scrollY, true);
        }
        finally {
            qPainter.end();
        }
    }

    @Override
    public void repaintTextArea() {
        surface().viewport().repaint();
    }

    @Override
    public void repaintGutter() {
        surface().getGutter().repaint();
    }

    @Override
    public boolean hasErrorStripe() {
        DesktopQtEditorErrorStripeWidget errorStripe = myEditor.getErrorStripe();
        return errorStripe != null && errorStripe.isStripeVisible() && errorStripe.width() > 0 && errorStripe.height() > 0;
    }

    @Override
    public void repaintErrorStripe() {
        Objects.requireNonNull(myEditor.getErrorStripe()).repaint();
    }

    @Override
    public void resetPaint() {
        myWatcher.reset();
    }

    @Override
    public void flushPaint(long deadlineNanos) {
        long timeout = Math.max(1, (deadlineNanos - System.nanoTime()) / 1_000_000L);
        QTimer.singleShot((int) timeout, Qt.TimerType.PreciseTimer, () -> {
        });

        while (!myWatcher.isPainted() && System.nanoTime() < deadlineNanos) {
            myEventLoop.processEvents(QEventLoop.ProcessEventsFlag.ExcludeUserInputEvents, QEventLoop.ProcessEventsFlag.WaitForMoreEvents);
        }
    }

    @Override
    public long getPaintedPixels() {
        return myWatcher.getPaintedPixels();
    }

    private QImage offscreenImage(QWidget viewport) {
        double ratio = surface().devicePixelRatioF();
        int width = (int) Math.ceil(viewport.width() * ratio);
        int height = (int) Math.ceil(viewport.height() * ratio);

        QImage image = myImage;
        if (image == null || image.width() != width || image.height() != height) {
            if (image != null) {
                image.dispose();
            }
            image = new QImage(width, height, QImage.Format.Format_ARGB32_Premultiplied);
            image.setDevicePixelRatio(ratio);
            myImage = image;
        }
        return image;
    }

    private DesktopQtEditorWidget surface() {
        return Objects.requireNonNull(myEditor.getSurface());
    }
}
