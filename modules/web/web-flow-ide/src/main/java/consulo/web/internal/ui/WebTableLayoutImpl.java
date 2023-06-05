/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.ui;


import com.vaadin.flow.component.HasSize;
import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.vaadin.stefan.table.Table;
import org.vaadin.stefan.table.TableDataCell;
import org.vaadin.stefan.table.TableRow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-08-25
 */
public class WebTableLayoutImpl extends VaadinComponentDelegate<WebTableLayoutImpl.Vaadin> implements TableLayout {

  public class Vaadin extends Table implements FromVaadinComponentWrapper {
    private final Map<Component, TableCell> myChildren = new LinkedHashMap<>();

    public void add(@Nonnull Component component, TableCell cell) {
      myChildren.put(component, cell);

      com.vaadin.flow.component.Component vComponent = TargetVaddin.to(component);
      ((HasSize)vComponent).setSizeFull();
      validate(cell).add(vComponent);
    }

    private TableDataCell validate(TableCell tableCell) {
      int rowIndex = tableCell.getRow();
      int columnIndex = tableCell.getColumn();

      int rowSize = rowIndex + 1;
      int columnSize = columnIndex + 1;

      List<TableRow> rows = getRows();
      if (rows.size() < rowSize) {
        addRows(rowSize - rows.size());
      }

      TableRow row = getRow(rowIndex).get();

      List<TableDataCell> dataCells = row.getDataCells();
      if (dataCells.size() < columnSize) {
        row.addDataCells(columnSize - dataCells.size());
      }

      return row.getDataCell(columnIndex).get();
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebTableLayoutImpl.this;
    }
  }

  public WebTableLayoutImpl(StaticPosition fillOption) {
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TableLayout add(@Nonnull consulo.ui.Component component, @Nonnull TableCell tableCell) {
    toVaadinComponent().add(component, tableCell);
    return this;
  }
}
