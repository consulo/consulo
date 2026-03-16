/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface TableLayout extends Layout<TableLayout.TableCell> {
    static class TableCell implements LayoutConstraint {
        private int myRow;
        private int myColumn;

        private boolean myFill;

        private TableCell(int row, int column) {
            myRow = row;
            myColumn = column;
        }

        public boolean isFill() {
            return myFill;
        }

        public int getRow() {
            return myRow;
        }

        public int getColumn() {
            return myColumn;
        }
        public TableCell fill() {
            myFill = true;
            return this;
        }
    }

    static TableLayout create(StaticPosition fillOption) {
        return UIInternal.get()._Layouts_table(fillOption);
    }
    static TableCell cell(int row, int column) {
        return new TableCell(row, column);
    }
    @RequiredUIAccess
    default TableLayout add(PseudoComponent pseudoComponent, TableCell tableCell) {
        return add(pseudoComponent.getComponent(), tableCell);
    }
    @Override
    default TableLayout add(Component component, TableCell constraint) {
        return (TableLayout) Layout.super.add(component, constraint);
    }
}
