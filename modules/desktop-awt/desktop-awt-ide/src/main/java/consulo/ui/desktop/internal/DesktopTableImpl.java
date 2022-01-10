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
package consulo.ui.desktop.internal;

import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.model.TableModel;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
class DesktopTableImpl<Item> extends SwingComponentDelegate<DesktopTableImpl.MyTableView> implements Table<Item> {
  class MyTableView<K> extends TableView<K> implements FromSwingComponentWrapper {
    MyTableView(ListTableModel<K> model) {
      super(model);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopTableImpl.this;
    }
  }

  @SuppressWarnings("unchecked")
  public DesktopTableImpl(@Nonnull Iterable<? extends TableColumn> columns, @Nonnull TableModel<Item> model) {
    List<ColumnInfo<Item, ?>> cols = new ArrayList<>();
    for (TableColumn column : columns) {
      cols.add((ColumnInfo<Item, ?>)column);
    }

    ColumnInfo<Item, ?>[] array = cols.toArray(new ColumnInfo[cols.size()]);

    DesktopTableModelImpl tableModel = (DesktopTableModelImpl)model;
    tableModel.setColumnInfos(array);

    MyTableView<Item> tableView = new MyTableView<>(tableModel);

    initialize(tableView);
  }
}
