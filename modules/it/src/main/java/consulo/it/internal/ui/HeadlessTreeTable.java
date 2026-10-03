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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.TableColumn;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeTableColumns;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessTreeTable<E> extends HeadlessTree<E> implements TreeTable<E>, TableColumnOwner {
    private final TreeTableColumns<E> myColumns = new TreeTableColumns<>();

    private volatile LocalizeValue myTreeColumnHeader = LocalizeValue.empty();

    public HeadlessTreeTable(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);
        getController().setColumnValueFactory(myColumns::values);
    }

    public TreeTableColumns<E> getTreeTableColumns() {
        return myColumns;
    }

    public LocalizeValue getTreeColumnHeader() {
        return myTreeColumnHeader;
    }

    public List<LocalizeValue> getHeaders() {
        List<LocalizeValue> headers = new ArrayList<>();
        headers.add(myTreeColumnHeader);
        for (TableColumnImpl<E, ?> column : myColumns.getColumns()) {
            headers.add(column.getHeader());
        }
        return headers;
    }

    @Override
    public void setTreeColumnHeader(LocalizeValue header) {
        myTreeColumnHeader = header;
    }

    @Override
    public <V> TableColumn<E, V> addColumn(LocalizeValue header, Function<E, V> valueProvider) {
        TableColumnImpl<E, V> column = myColumns.add(this, header, valueProvider);
        getController().refreshColumnValues();
        return column;
    }

    @Override
    public List<TableColumn<E, ?>> getColumns() {
        return myColumns.getTableColumns();
    }

    @RequiredUIAccess
    public CompletableFuture<?> clickHeader(int columnIndex) {
        return getController().setSortComparator(myColumns.cycleSort(columnIndex));
    }

    @RequiredUIAccess
    public CompletableFuture<?> sortBy(int columnIndex, boolean ascending) {
        return getController().setSortComparator(myColumns.sort(columnIndex, ascending));
    }

    @RequiredUIAccess
    public CompletableFuture<?> clearSort() {
        return getController().setSortComparator(myColumns.sort(-1, true));
    }

    @RequiredUIAccess
    public String renderCell(TreeNodeImpl<E> node, int columnIndex, boolean selected) {
        HeadlessTextItemPresentation presentation = new HeadlessTextItemPresentation();
        myColumns.render(columnIndex, presentation, node, selected);
        return presentation.getText();
    }

    @Override
    public void headerChanged() {
    }

    @Override
    public void renderChanged() {
    }

    @Override
    public void layoutChanged() {
    }

    @Override
    public void sortChanged() {
        getController().setSortComparator(myColumns.getSortComparator());
    }
}
