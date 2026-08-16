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
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import io.qt.core.Qt;
import io.qt.widgets.QGridLayout;
import io.qt.widgets.QLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTableLayoutImpl extends DesktopQtLayoutComponent<TableLayout.TableCell, DesktopQtTableLayoutImpl.Cell>
    implements TableLayout {
    public record Cell(int row, int column, boolean fill) {
    }

    private final StaticPosition myFillOption;

    /** the row the leftover height is parked in, so it can be moved along as the table grows */
    private int myFillerRow = -1;

    public DesktopQtTableLayoutImpl(StaticPosition fillOption) {
        myFillOption = fillOption;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return new QGridLayout();
    }

    @Override
    public Cell convertConstraintsToLayoutData(TableCell constraint) {
        return new Cell(constraint.getRow(), constraint.getColumn(), constraint.isFill());
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QGridLayout layout = (QGridLayout) myComponent.layout();

        Cell cell = layoutData instanceof Cell value ? value : new Cell(0, 0, false);

        // the awt table anchors west and only stretches a cell which asked to be filled, which is how a label
        // beside a text box keeps its own width and sits level with it
        Qt.Alignment alignment = cell.fill()
            ? new Qt.Alignment()
            : new Qt.Alignment(Qt.AlignmentFlag.AlignLeft, Qt.AlignmentFlag.AlignVCenter);

        layout.addWidget(child.toQtComponent(), cell.row(), cell.column(), alignment);

        if (cell.fill()) {
            layout.setColumnStretch(cell.column(), 1);
        }

        updateFiller(layout);
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        super.detach(child);

        if (myComponent != null && myComponent.layout() instanceof QGridLayout layout) {
            updateFiller(layout);
        }
    }

    /**
     * A grid hands the height it has left over to its rows, where the api asks for rows of their own height and
     * the remainder parked at one side - the web frontend answers this with a filler row of the full height below
     * the last one, and an empty row carrying the whole of the stretch is the same thing here.
     */
    private void updateFiller(QGridLayout layout) {
        if (myFillOption == StaticPosition.CENTER) {
            return;
        }

        int lastRow = -1;
        for (int i = 0; i < layout.count(); i++) {
            QGridLayout.ItemInfo position = layout.getItemPosition(i);

            lastRow = Math.max(lastRow, position.row + position.rowSpan - 1);
        }

        int fillerRow = lastRow + 1;
        if (fillerRow == myFillerRow) {
            return;
        }

        if (myFillerRow != -1) {
            layout.setRowStretch(myFillerRow, 0);
        }

        myFillerRow = fillerRow;

        layout.setRowStretch(fillerRow, 1);
    }

    @Override
    public void disposeQt() {
        myFillerRow = -1;

        super.disposeQt();
    }

    @Override
    @RequiredUIAccess
    public TableLayout add(Component component, TableCell tableCell) {
        addImpl(component, convertConstraintsToLayoutData(tableCell));
        return this;
    }
}
