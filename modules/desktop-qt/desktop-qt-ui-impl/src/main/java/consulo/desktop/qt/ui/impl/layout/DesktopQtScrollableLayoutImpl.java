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
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLayout;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtScrollableLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements ScrollableLayout {
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
        return scrollArea;
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
        ((QScrollArea) myComponent).setWidget(child.toQtComponent());
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        if (myComponent instanceof QScrollArea scrollArea && scrollArea.widget() == child.toQtComponent()) {
            scrollArea.takeWidget();
        }
    }
}
