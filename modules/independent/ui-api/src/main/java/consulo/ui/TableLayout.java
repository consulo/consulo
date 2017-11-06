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
package consulo.ui;

import consulo.ui.shared.StaticPosition;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface TableLayout extends Layout {
  static class TableCell {
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

    @NotNull
    public TableCell fill() {
      myFill = true;
      return this;
    }
  }

  static TableLayout create(@NotNull StaticPosition fillOption) {
    return UIInternal.get()._Layouts_table(fillOption);
  }

  @NotNull
  static TableCell cell(int row, int column) {
    return new TableCell(row, column);
  }

  @NotNull
  @RequiredUIAccess
  default TableLayout add(@NotNull PseudoComponent pseudoComponent, @NotNull TableCell tableCell) {
    return add(pseudoComponent.getComponent(), tableCell);
  }

  @NotNull
  @RequiredUIAccess
  TableLayout add(@NotNull Component component, @NotNull TableCell tableCell);
}
