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
package consulo.desktop.qt.ui.impl;

import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.widgets.QAbstractScrollArea;
import io.qt.widgets.QSplitter;
import io.qt.widgets.QTabBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopQtSpotlightOverlay extends QWidget {
    private static final int STROKE = 2;
    private static final int HORIZONTAL_INSET = 8;
    private static final int VERTICAL_INSET = 6;
    private static final int SECTION_VERTICAL_INSET = 2;
    private static final int TAB_INSET = 1;
    private static final int MAX_ARC = 30;
    private static final int DIM_ALPHA = 100;
    private static final int DARKER_FACTOR = 143;

    private final QWidget myHost;

    private final Supplier<List<DesktopQtSpotlightHole>> myTargets;

    private Set<QWidget> myWatched = new LinkedHashSet<>();

    private boolean myRewatchQueued;
    private boolean myRaiseQueued;

    private DesktopQtSpotlightOverlay(QWidget container, QWidget host, Supplier<List<DesktopQtSpotlightHole>> targets) {
        super(container);
        myHost = host;
        myTargets = targets;

        setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, true);
        setAttribute(Qt.WidgetAttribute.WA_NoSystemBackground, true);
        setFocusPolicy(Qt.FocusPolicy.NoFocus);
        setStyleSheet("background: transparent; border: none;");

        host.installEventFilter(this);

        syncGeometry();
    }

    public static DesktopQtSpotlightOverlay create(QWidget host, Supplier<List<DesktopQtSpotlightHole>> targets) {
        QWidget parent = host.parentWidget();
        QWidget container = host instanceof QSplitter && parent != null ? parent : host;
        return new DesktopQtSpotlightOverlay(container, host, targets);
    }

    public QWidget getHost() {
        return myHost;
    }

    public void refresh(boolean lit) {
        if (isDisposed()) {
            return;
        }

        rewatch();

        if (lit) {
            syncGeometry();
            raise();
        }

        setVisible(lit);
        update();
    }

    public void detach() {
        if (isDisposed()) {
            return;
        }

        unwatch(myWatched);
        myWatched = new LinkedHashSet<>();

        if (!myHost.isDisposed()) {
            myHost.removeEventFilter(this);
        }

        hide();
        disposeLater();
    }

    @Override
    public boolean eventFilter(QObject watched, QEvent event) {
        QEvent.Type type = event.type();

        if (watched == myHost) {
            if (type == QEvent.Type.Resize || type == QEvent.Type.Move) {
                syncGeometry();
            }
            else if (type == QEvent.Type.ChildAdded && parentWidget() == myHost) {
                queueRaise();
            }
        }

        if (type == QEvent.Type.Move
            || type == QEvent.Type.Resize
            || type == QEvent.Type.Show
            || type == QEvent.Type.Hide
            || type == QEvent.Type.LayoutRequest) {
            update();
        }

        return false;
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        List<DesktopQtSpotlightHole> targets = myTargets.get();

        Set<QWidget> chain = collectChain(targets);
        if (!chain.equals(myWatched)) {
            queueRewatch();
        }

        QPainterPath holes = new QPainterPath();
        for (DesktopQtSpotlightHole target : targets) {
            QPainterPath hole = getHole(target);
            if (hole != null) {
                holes = holes.united(hole);
            }
        }

        QPainterPath dim = new QPainterPath();
        dim.setFillRule(Qt.FillRule.OddEvenFill);
        dim.addRect(new QRectF(rect()));
        dim.addPath(holes);

        QColor dimColor = myHost.palette().color(QPalette.ColorRole.Window).darker(DARKER_FACTOR);
        dimColor.setAlpha(DIM_ALPHA);

        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.fillPath(dim, dimColor);
            painter.strokePath(holes, new QPen(new QColor(255, 200, 0, DIM_ALPHA), STROKE));
        }
        finally {
            painter.end();
        }
    }

    private @Nullable QPainterPath getHole(DesktopQtSpotlightHole hole) {
        QWidget target = hole.widget();
        if (target.isDisposed() || target == myHost || !target.isVisible() || !myHost.isAncestorOf(target)) {
            return null;
        }

        QRect area;
        int horizontalInset;
        int verticalInset;
        QRect clipRect = getClip(target);
        if (hole.isTab()) {
            if (!(target instanceof QTabBar tabBar) || hole.tabIndex() >= tabBar.count() || !tabBar.isTabVisible(hole.tabIndex())) {
                return null;
            }

            QRect tabRect = tabBar.tabRect(hole.tabIndex());
            QPoint origin = toOverlay(tabBar, tabRect.topLeft());
            area = new QRect(origin.x(), origin.y(), tabRect.width(), tabRect.height());
            horizontalInset = TAB_INSET;
            verticalInset = TAB_INSET;

            QPoint barOrigin = toOverlay(tabBar, new QPoint(0, 0));
            clipRect = clipRect.intersected(new QRect(barOrigin.x(), barOrigin.y(), tabBar.width(), tabBar.height()));
        }
        else {
            QPoint origin = toOverlay(target, new QPoint(0, 0));
            area = new QRect(origin.x(), origin.y(), target.width(), target.height());
            horizontalInset = HORIZONTAL_INSET;
            verticalInset = hole.section() ? SECTION_VERTICAL_INSET : VERTICAL_INSET;
        }

        int height = area.height() + verticalInset * 2;

        QPainterPath path = new QPainterPath();
        path.addRoundedRect(
            new QRectF(area.x() - horizontalInset, area.y() - verticalInset, area.width() + horizontalInset * 2, height),
            Math.min(height, MAX_ARC) / 2.0,
            Math.min(height, MAX_ARC) / 2.0
        );

        QPainterPath clip = new QPainterPath();
        clip.addRect(new QRectF(clipRect));
        return path.intersected(clip);
    }

    private QRect getClip(QWidget target) {
        QRect clip = rect();
        for (QWidget widget = target.parentWidget(); widget != null && widget != myHost; widget = widget.parentWidget()) {
            QWidget parent = widget.parentWidget();
            if (parent instanceof QAbstractScrollArea scrollArea && scrollArea.viewport() == widget) {
                QPoint origin = toOverlay(widget, new QPoint(0, 0));
                clip = clip.intersected(new QRect(origin.x(), origin.y(), widget.width(), widget.height()));
            }
        }
        return clip;
    }

    private QPoint toOverlay(QWidget widget, QPoint point) {
        QWidget container = parentWidget();
        QPoint inContainer = container == null ? point : widget.mapTo(container, point);
        return new QPoint(inContainer.x() - x(), inContainer.y() - y());
    }

    private void syncGeometry() {
        QWidget container = parentWidget();
        if (container == null || myHost.isDisposed()) {
            return;
        }

        if (container == myHost) {
            setGeometry(myHost.rect());
        }
        else {
            QPoint origin = myHost.mapTo(container, new QPoint(0, 0));
            setGeometry(origin.x(), origin.y(), myHost.width(), myHost.height());
        }
    }

    private Set<QWidget> collectChain(List<DesktopQtSpotlightHole> holes) {
        Set<QWidget> chain = new LinkedHashSet<>();
        for (DesktopQtSpotlightHole hole : holes) {
            QWidget target = hole.widget();
            if (target.isDisposed() || !myHost.isAncestorOf(target)) {
                continue;
            }

            for (QWidget widget = target; widget != null && widget != myHost; widget = widget.parentWidget()) {
                chain.add(widget);
            }
        }
        return chain;
    }

    private void rewatch() {
        Set<QWidget> chain = collectChain(myTargets.get());

        Set<QWidget> stale = new LinkedHashSet<>(myWatched);
        stale.removeAll(chain);
        unwatch(stale);

        for (QWidget widget : chain) {
            if (!myWatched.contains(widget)) {
                widget.installEventFilter(this);
            }
        }

        myWatched = chain;
    }

    private void unwatch(Set<QWidget> widgets) {
        for (QWidget widget : widgets) {
            if (!widget.isDisposed()) {
                widget.removeEventFilter(this);
            }
        }
    }

    private void queueRewatch() {
        if (myRewatchQueued) {
            return;
        }

        myRewatchQueued = true;

        DesktopQtUIAccess.INSTANCE.execute(() -> {
            if (isDisposed()) {
                return;
            }

            myRewatchQueued = false;

            rewatch();
            update();
        });
    }

    private void queueRaise() {
        if (myRaiseQueued) {
            return;
        }

        myRaiseQueued = true;

        DesktopQtUIAccess.INSTANCE.execute(() -> {
            if (isDisposed()) {
                return;
            }

            myRaiseQueued = false;

            if (isVisible()) {
                raise();
            }
        });
    }
}
