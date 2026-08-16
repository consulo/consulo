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
public class DesktopQtDockLayoutImpl extends DesktopQtLayoutComponent<StaticPosition, DesktopQtDockLayoutImpl.GridCell>
    implements DockLayout {
    public record GridCell(int row, int column, int rowSpan, int columnSpan) {
    }

    private static final GridCell TOP_CELL = new GridCell(0, 0, 1, 3);
    private static final GridCell LEFT_CELL = new GridCell(1, 0, 1, 1);
    private static final GridCell CENTER_CELL = new GridCell(1, 1, 1, 1);
    private static final GridCell RIGHT_CELL = new GridCell(1, 2, 1, 1);
    private static final GridCell BOTTOM_CELL = new GridCell(2, 0, 1, 3);

    private final int myGap;

    public DesktopQtDockLayoutImpl(int gapInPixels) {
        myGap = gapInPixels;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        QGridLayout layout = new QGridLayout();
        layout.setSpacing(myGap);
        layout.setRowStretch(1, 1);
        layout.setColumnStretch(1, 1);
        return layout;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QGridLayout layout = (QGridLayout) myComponent.layout();

        QWidget widget = child.toQtComponent();

        GridCell cell = layoutData instanceof GridCell gridCell ? gridCell : CENTER_CELL;

        layout.addWidget(widget, cell.row(), cell.column(), cell.rowSpan(), cell.columnSpan());
    }

    @Override
    public GridCell convertConstraintsToLayoutData(StaticPosition constraint) {
        return switch (constraint) {
            case TOP -> TOP_CELL;
            case BOTTOM -> BOTTOM_CELL;
            case LEFT -> LEFT_CELL;
            case RIGHT -> RIGHT_CELL;
            case CENTER -> CENTER_CELL;
        };
    }
}
