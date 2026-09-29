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

import consulo.ui.ex.localize.UILocalize;
import io.qt.core.QEvent;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import io.qt.widgets.QTabBar;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
final class DesktopQtTabCloseButton extends QAbstractButton {
    private static final int TRAILING_INSET = 6;

    DesktopQtTabCloseButton(QTabBar tabBar) {
        super(tabBar);

        setFocusPolicy(Qt.FocusPolicy.NoFocus);
        setCursor(Qt.CursorShape.ArrowCursor);
        setToolTip(UILocalize.tabbedPaneCloseTabActionName().get());

        resize(sizeHint());
    }

    private int indicatorWidth() {
        return style().pixelMetric(QStyle.PixelMetric.PM_TabCloseIndicatorWidth, null, this);
    }

    @Override
    public QSize sizeHint() {
        ensurePolished();

        return new QSize(
            indicatorWidth() + TRAILING_INSET,
            style().pixelMetric(QStyle.PixelMetric.PM_TabCloseIndicatorHeight, null, this)
        );
    }

    @Override
    protected void enterEvent(QEnterEvent event) {
        super.enterEvent(event);

        update();
    }

    @Override
    protected void leaveEvent(QEvent event) {
        super.leaveEvent(event);

        update();
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QStyleOption option = new QStyleOption();
        option.initFrom(this);
        option.setRect(new QRect(0, 0, indicatorWidth(), height()));

        QStyle.State state = option.state();
        state.setFlag(QStyle.StateFlag.State_AutoRaise, true);
        state.setFlag(QStyle.StateFlag.State_Raised, isEnabled() && underMouse() && !isChecked() && !isDown());
        state.setFlag(QStyle.StateFlag.State_On, isChecked());
        state.setFlag(QStyle.StateFlag.State_Sunken, isDown());
        state.setFlag(QStyle.StateFlag.State_Selected, isOnCurrentTab());
        option.setState(state);

        QPainter painter = new QPainter(this);
        try {
            style().drawPrimitive(QStyle.PrimitiveElement.PE_IndicatorTabClose, option, painter, this);
        }
        finally {
            painter.end();
        }
    }

    private boolean isOnCurrentTab() {
        if (!(parentWidget() instanceof QTabBar tabBar)) {
            return false;
        }

        int index = tabBar.currentIndex();
        return index != -1 && tabBar.tabButton(index, QTabBar.ButtonPosition.RightSide) == this;
    }
}
