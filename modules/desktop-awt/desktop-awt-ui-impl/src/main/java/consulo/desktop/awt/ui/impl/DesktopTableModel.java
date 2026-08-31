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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.model.FlatDataModel;

import java.util.AbstractList;

/**
 * @author VISTALL
 * @since 2020-11-23
 */
class DesktopTableModel<Item> extends ListTableModel<Item> {
    DesktopTableModel(FlatDataModel<Item> model) {
        // a live view rather than a copy, so the swing model can never drift out of sync with the
        // consulo one
        super(new ColumnInfo[0], new AbstractList<Item>() {
            @Override
            public Item get(int index) {
                return model.get(index);
            }

            @Override
            public int size() {
                return model.getSize();
            }
        });

        setSortable(true);

        model.addListener(event -> {
            int from = event.getFromIndex();
            int to = event.getToIndex();

            switch (event.getType()) {
                case ADDED -> fireTableRowsInserted(from, to);
                case REMOVED -> fireTableRowsDeleted(from, to);
                case UPDATED -> fireTableRowsUpdated(from, to);
                case RESET -> fireTableDataChanged();
            }
        });
    }
}
