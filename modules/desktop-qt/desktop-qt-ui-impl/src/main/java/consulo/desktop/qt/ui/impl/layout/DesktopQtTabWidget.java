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

import io.qt.core.QSize;
import io.qt.gui.QIcon;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionTab;
import io.qt.widgets.QTabBar;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
final class DesktopQtTabWidget extends QTabWidget {
    private static final class TabBar extends QTabBar {
        @Override
        protected QSize tabSizeHint(int index) {
            QSize size = super.tabSizeHint(index);
            if (!tabIcon(index).isNull()) {
                size.setWidth(size.width() - redundantIconRoom(index));
            }
            return size;
        }

        private int redundantIconRoom(int index) {
            QStyleOptionTab option = new QStyleOptionTab();
            initStyleOption(option, index);

            QSize withIcon = style().sizeFromContents(QStyle.ContentsType.CT_TabBarTab, option, new QSize(0, 0), this);

            option.setIcon(new QIcon());

            QSize withoutIcon = style().sizeFromContents(QStyle.ContentsType.CT_TabBarTab, option, new QSize(0, 0), this);

            return Math.max(0, withIcon.width() - withoutIcon.width());
        }
    }

    DesktopQtTabWidget(QWidget parent) {
        super(parent);

        setTabBar(new TabBar());
    }
}
