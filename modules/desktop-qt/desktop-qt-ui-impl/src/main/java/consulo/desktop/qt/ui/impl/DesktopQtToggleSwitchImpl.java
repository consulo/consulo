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

import consulo.ui.ToggleSwitch;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.core.QEasingCurve;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.QVariantAnimation;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QCheckBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtToggleSwitchImpl extends QtComponentDelegate<DesktopQtToggleSwitchImpl.QtSwitch> implements ToggleSwitch {
    public static class QtSwitch extends QCheckBox {
        private static final int HEIGHT = 22;
        private static final int WIDTH = 44;
        private static final int MARGIN = 3;

        private final QVariantAnimation myAnimation = new QVariantAnimation();

        private int myThumbLeft = MARGIN;

        public QtSwitch(QWidget parent) {
            super(parent);

            setFixedSize(new QSize(WIDTH, HEIGHT));
            setCursor(new io.qt.gui.QCursor(Qt.CursorShape.PointingHandCursor));

            myAnimation.setDuration(120);
            myAnimation.setEasingCurve(QEasingCurve.Type.OutCubic);
            myAnimation.valueChanged.connect(value -> {
                myThumbLeft = (Integer) value;
                update();
            });

            toggled.connect(this::animateTo);
        }

        private void animateTo(boolean checked) {
            myAnimation.stop();
            myAnimation.setStartValue(myThumbLeft);
            myAnimation.setEndValue(checked ? WIDTH - HEIGHT + MARGIN : MARGIN);
            myAnimation.start();
        }

        @Override
        public QSize sizeHint() {
            return new QSize(WIDTH, HEIGHT);
        }

        @Override
        protected boolean hitButton(QPoint pos) {
            return contentsRect().contains(pos);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPalette palette = palette();
            QPalette.ColorGroup group = isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled;

            QColor track = palette.color(group, isChecked() ? QPalette.ColorRole.Highlight : QPalette.ColorRole.Mid);
            QColor thumb = palette.color(group, isChecked() ? QPalette.ColorRole.HighlightedText : QPalette.ColorRole.Window);

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                painter.setPen(Qt.PenStyle.NoPen);

                painter.setBrush(track);
                painter.drawRoundedRect(new QRect(0, 0, WIDTH, HEIGHT), HEIGHT / 2.0, HEIGHT / 2.0);

                painter.setBrush(thumb);
                painter.drawEllipse(myThumbLeft, MARGIN, HEIGHT - 2 * MARGIN, HEIGHT - 2 * MARGIN);
            }
            finally {
                painter.end();
            }
        }
    }

    private boolean myValue;

    private boolean myFireListeners = true;

    public DesktopQtToggleSwitchImpl(boolean selected) {
        myValue = selected;
    }

    @Override
    protected QtSwitch createQt(QWidget parent) {
        return new QtSwitch(parent);
    }

    @Override
    protected void initialize(QtSwitch component) {
        super.initialize(component);

        component.setChecked(myValue);
        component.myThumbLeft = myValue ? QtSwitch.WIDTH - QtSwitch.HEIGHT + QtSwitch.MARGIN : QtSwitch.MARGIN;

        component.toggled.connect(checked -> {
            myValue = checked;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myValue, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public Boolean getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable Boolean value, boolean fireListeners) {
        myValue = value != null && value;

        if (myComponent != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setChecked(myValue);
            }
            finally {
                myFireListeners = true;
            }
        }
    }
}
