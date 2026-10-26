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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.internal.ScrollableLayoutViewportEvent;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalScrollBarPosition;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLayout;
import io.qt.widgets.QAbstractScrollArea;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtScrollableLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements ScrollableLayoutInternal {
    private static class ClippingScrollArea extends QScrollArea {
        ClippingScrollArea(QWidget parent) {
            super(parent);
        }

        @Override
        public QSize sizeHint() {
            QWidget content = widget();
            QSize size = content == null ? new QSize(0, 0) : content.sizeHint();
            int frame = frameWidth() * 2;
            return new QSize(size.width() + frame, size.height() + frame);
        }

        @Override
        public QSize minimumSizeHint() {
            QWidget content = widget();
            int frame = frameWidth() * 2;
            return new QSize(frame, (content == null ? 0 : content.minimumSizeHint().height()) + frame);
        }
    }

    private final ScrollableLayoutOptions myOptions;

    private final Map<Component, @Nullable QWidget> myTracked = new HashMap<>();

    private @Nullable Component myOverlay;

    public DesktopQtScrollableLayoutImpl(Component component, ScrollableLayoutOptions options) {
        myOptions = options;

        addImpl(component, null);
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        boolean clipping = myOptions.getHorizontalScrollPolicy() == ScrollableLayoutOptions.ScrollPolicy.NEVER
            && myOptions.getVerticalScrollPolicy() == ScrollableLayoutOptions.ScrollPolicy.NEVER;

        QScrollArea scrollArea = clipping ? new ClippingScrollArea(parent) : new QScrollArea(parent);
        scrollArea.setWidgetResizable(true);
        scrollArea.setFrameShape(QFrame.Shape.NoFrame);
        scrollArea.setHorizontalScrollBarPolicy(toQt(myOptions.getHorizontalScrollPolicy()));
        scrollArea.setVerticalScrollBarPolicy(toQt(myOptions.getVerticalScrollPolicy()));
        if (myOptions.getVerticalScrollBarPosition() == VerticalScrollBarPosition.LEFT) {
            scrollArea.setLayoutDirection(Qt.LayoutDirection.RightToLeft);
            scrollArea.viewport().setLayoutDirection(Qt.LayoutDirection.LeftToRight);
            scrollArea.horizontalScrollBar().setLayoutDirection(Qt.LayoutDirection.LeftToRight);
        }

        scrollArea.verticalScrollBar().valueChanged.connect(value -> fireViewportChanged());
        scrollArea.horizontalScrollBar().valueChanged.connect(value -> fireViewportChanged());
        scrollArea.verticalScrollBar().rangeChanged.connect((min, max) -> fireViewportChanged());
        scrollArea.horizontalScrollBar().rangeChanged.connect((min, max) -> fireViewportChanged());
        scrollArea.viewport().installEventFilter(new GeometryFilter(scrollArea.viewport()));
        return scrollArea;
    }

    private class GeometryFilter extends QObject {
        GeometryFilter(QObject parent) {
            super(parent);
        }

        @Override
        public boolean eventFilter(QObject watched, QEvent event) {
            QEvent.Type type = event.type();
            if (type == QEvent.Type.Resize || type == QEvent.Type.Move) {
                layoutOverlay();
                fireViewportChanged();
            }
            return false;
        }
    }

    private void fireViewportChanged() {
        if (!(myComponent instanceof QScrollArea)) {
            return;
        }
        getListenerDispatcher(ScrollableLayoutViewportEvent.class).onEvent(new ScrollableLayoutViewportEvent(this, getViewRect()));
    }

    @RequiredUIAccess
    @Override
    public Rectangle2D getViewRect() {
        if (!(myComponent instanceof QScrollArea scrollArea) || !isAlive(scrollArea)) {
            return new Rectangle2D(0, 0);
        }

        QAbstractScrollArea inner = findSelfScrollingContent(scrollArea);
        if (inner != null) {
            QWidget innerViewport = inner.viewport();
            return new Rectangle2D(
                inner.horizontalScrollBar().value(),
                inner.verticalScrollBar().value(),
                innerViewport.width(),
                innerViewport.height()
            );
        }

        QWidget viewport = scrollArea.viewport();
        return new Rectangle2D(
            scrollArea.horizontalScrollBar().value(),
            scrollArea.verticalScrollBar().value(),
            viewport.width(),
            viewport.height()
        );
    }

    @RequiredUIAccess
    @Override
    public Size2D getContentSize() {
        if (!(myComponent instanceof QScrollArea scrollArea) || !isAlive(scrollArea)) {
            return new Size2D(0, 0);
        }

        QAbstractScrollArea inner = findSelfScrollingContent(scrollArea);
        if (inner != null) {
            QWidget innerViewport = inner.viewport();
            return new Size2D(
                inner.horizontalScrollBar().maximum() + innerViewport.width(),
                inner.verticalScrollBar().maximum() + innerViewport.height()
            );
        }

        QWidget content = scrollArea.widget();
        return content == null ? new Size2D(0, 0) : new Size2D(content.width(), content.height());
    }

    /**
     * A content which scrolls itself - a list - fills the scroll area, so what scrolls is the content rather than the
     * area around it.
     */
    private static @Nullable QAbstractScrollArea findSelfScrollingContent(QScrollArea scrollArea) {
        QWidget content = scrollArea.widget();
        return content instanceof QAbstractScrollArea inner && isAlive(inner) ? inner : null;
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Point2D position) {
        if (!(myComponent instanceof QScrollArea scrollArea) || !isAlive(scrollArea)) {
            return;
        }

        scrollArea.horizontalScrollBar().setValue(position.x());
        scrollArea.verticalScrollBar().setValue(position.y());
    }

    @RequiredUIAccess
    @Override
    public void setBoundsTracked(Component descendant, boolean tracked) {
        if (tracked) {
            if (!myTracked.containsKey(descendant)) {
                myTracked.put(descendant, null);
            }
            installGeometryFilter(descendant);
        }
        else {
            myTracked.remove(descendant);
        }
    }

    private @Nullable QWidget installGeometryFilter(Component descendant) {
        if (!(descendant instanceof QtComponentDelegate<?> delegate)) {
            return null;
        }

        QWidget widget = delegate.toQtComponent();
        if (widget == null || !isAlive(widget)) {
            return null;
        }

        if (myTracked.get(descendant) != widget) {
            widget.installEventFilter(new GeometryFilter(widget));
            myTracked.put(descendant, widget);
        }
        return widget;
    }

    @RequiredUIAccess
    @Override
    public void setTopOverlay(@Nullable Component overlay) {
        Component old = myOverlay;
        if (old instanceof QtComponentDelegate<?> oldDelegate) {
            QWidget oldWidget = oldDelegate.toQtComponent();
            if (oldWidget != null && isAlive(oldWidget)) {
                oldWidget.hide();
                oldWidget.setParent(null);
            }
        }
        myOverlay = overlay;
        layoutOverlay();
    }

    private void layoutOverlay() {
        if (!(myOverlay instanceof QtComponentDelegate<?> delegate) || !(myComponent instanceof QScrollArea scrollArea) || !isAlive(scrollArea)) {
            return;
        }
        QWidget viewport = scrollArea.viewport();
        QWidget widget = delegate.toQtComponent();
        if (widget == null || !isAlive(widget)) {
            delegate.bind(viewport, null);
            widget = delegate.toQtComponent();
            if (widget == null) {
                return;
            }
        }
        else if (widget.parentWidget() != viewport) {
            widget.setParent(viewport);
        }
        int height = Math.max(widget.sizeHint().height(), widget.minimumSizeHint().height());
        widget.setGeometry(0, 0, viewport.width(), height);
        widget.raise();
        widget.show();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Rectangle2D getBounds(Component descendant) {
        if (!myTracked.containsKey(descendant) || !(myComponent instanceof QScrollArea scrollArea) || !isAlive(scrollArea)) {
            return null;
        }

        QWidget content = scrollArea.widget();
        QWidget widget = installGeometryFilter(descendant);
        if (widget == null || content == null || !content.isAncestorOf(widget)) {
            return null;
        }

        QPoint location = widget.mapTo(content, new QPoint(0, 0));
        return new Rectangle2D(location.x(), location.y(), widget.width(), widget.height());
    }

    private static Qt.ScrollBarPolicy toQt(ScrollableLayoutOptions.ScrollPolicy policy) {
        return switch (policy) {
            case ALWAYS -> Qt.ScrollBarPolicy.ScrollBarAlwaysOn;
            case IF_NEEDED -> Qt.ScrollBarPolicy.ScrollBarAsNeeded;
            case NEVER -> Qt.ScrollBarPolicy.ScrollBarAlwaysOff;
        };
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QWidget widget = child.toQtComponent();
        ((QScrollArea) myComponent).setWidget(widget);
        if (widget instanceof QAbstractScrollArea inner) {
            inner.verticalScrollBar().valueChanged.connect(value -> fireViewportChanged());
            inner.verticalScrollBar().rangeChanged.connect((min, max) -> fireViewportChanged());
        }
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        if (myComponent instanceof QScrollArea scrollArea && scrollArea.widget() == child.toQtComponent()) {
            scrollArea.takeWidget();
        }
    }
}
