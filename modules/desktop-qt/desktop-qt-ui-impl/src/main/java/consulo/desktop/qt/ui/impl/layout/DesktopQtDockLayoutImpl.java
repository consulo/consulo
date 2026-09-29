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

import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.layout.DockLayout;
import io.qt.widgets.QGridLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtDockLayoutImpl extends DesktopQtLayoutComponent<StaticPosition, StaticPosition>
    implements DockLayout {
    private final Space myGap;

    public DesktopQtDockLayoutImpl(Space gap) {
        myGap = gap;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        int gap = DesktopQtSpace.toPixels(myGap);

        QGridLayout middleRow = new QGridLayout();
        middleRow.setContentsMargins(0, 0, 0, 0);
        middleRow.setSpacing(gap);
        middleRow.setRowStretch(0, 1);
        middleRow.setColumnStretch(1, 1);

        QGridLayout rows = new QGridLayout();
        rows.setSpacing(gap);
        rows.setRowStretch(1, 1);
        rows.addLayout(middleRow, 1, 0);
        return rows;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        if (!isAlive(myComponent)) {
            return;
        }

        QGridLayout rows = (QGridLayout) myComponent.layout();

        QWidget widget = child.toQtComponent();

        StaticPosition position = layoutData instanceof StaticPosition staticPosition ? staticPosition : StaticPosition.CENTER;
        switch (position) {
            case TOP -> rows.addWidget(widget, 0, 0);
            case BOTTOM -> rows.addWidget(widget, 2, 0);
            case LEFT -> middleRowOf(rows).addWidget(widget, 0, 0);
            case CENTER -> middleRowOf(rows).addWidget(widget, 0, 1);
            case RIGHT -> middleRowOf(rows).addWidget(widget, 0, 2);
        }
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        super.detach(child);

        QWidget widget = child.toQtComponent();
        if (!isAlive(widget) || !isAlive(myComponent)) {
            return;
        }

        middleRowOf((QGridLayout) myComponent.layout()).removeWidget(widget);
    }

    private static QGridLayout middleRowOf(QGridLayout rows) {
        return (QGridLayout) rows.itemAtPosition(1, 0).layout();
    }

    @Override
    public StaticPosition convertConstraintsToLayoutData(StaticPosition constraint) {
        return constraint;
    }
}
