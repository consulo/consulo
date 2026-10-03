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
package consulo.desktop.awt.ui.impl;

import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.tree.DesktopAsyncTreeModel;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.TableColumn;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.concurrent.Promise;
import org.jspecify.annotations.Nullable;

import javax.swing.JScrollPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopTreeTableImpl<E> extends DesktopTreeBase<E, DesktopAWTTreeTable> implements TreeTable<E> {
    private static final Logger LOG = Logger.getInstance(DesktopTreeTableImpl.class);

    private static final int DEFAULT_MIN_WIDTH = 15;

    private final List<DesktopTableColumnImpl<E, ?>> myColumns = new CopyOnWriteArrayList<>();

    private LocalizeValue myTreeColumnHeader = LocalizeValue.empty();

    private volatile @Nullable DesktopTreeTableRowSorter<E> myRowSorter;
    private @Nullable Comparator<?> mySortValueComparator;

    private final Object myRebuildLock = new Object();
    private CompletableFuture<?> myRebuild = CompletableFuture.completedFuture(null);
    private final Set<DesktopTreeNodeImpl<E>> myPendingLevels = new LinkedHashSet<>();

    public DesktopTreeTableImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);
        getStructure().setColumnValueFactory(this::columnValues);
    }

    @Override
    protected DesktopAWTTreeTable createComponent() {
        DesktopAsyncTreeModel asyncModel = createAsyncTreeModel();
        DesktopTreeTableModel<E> tableModel = new DesktopTreeTableModel<>(asyncModel, this, destroyHook());
        DesktopAWTTree tree = createTree(asyncModel);
        DesktopAWTTreeTable treeTable = new DesktopAWTTreeTable(tableModel, tree, this);
        installTree(tree);

        treeTable.getTable().setAutoCreateColumnsFromModel(false);

        DesktopTreeTableRowSorter<E> sorter = new DesktopTreeTableRowSorter<>(treeTable, this);
        myRowSorter = sorter;
        treeTable.setRowSorter(sorter);

        applyColumns(treeTable);
        treeTable.updateColumnProportion();
        return treeTable;
    }

    @Override
    protected DesktopAWTTree getTree() {
        return toAWTComponent().getAWTTree();
    }

    @Override
    protected List<java.awt.Component> getInputComponents(DesktopAWTTreeTable component) {
        return List.of(component.getTree(), component.getTable());
    }

    @Override
    boolean isItemHeightSupported() {
        return false;
    }

    @Override
    public void setTreeColumnHeader(LocalizeValue header) {
        myTreeColumnHeader = header;

        if (isInitialized()) {
            toAWTComponent().repaint();
        }
    }

    LocalizeValue getTreeColumnHeader() {
        return myTreeColumnHeader;
    }

    @Override
    public <V> TableColumn<E, V> addColumn(LocalizeValue header, Function<E, V> valueProvider) {
        DesktopTableColumnImpl<E, V> column = new DesktopTableColumnImpl<>(item -> null, header, valueProvider);
        column.setChangeListener(() -> columnChanged(column));
        myColumns.add(column);

        if (isInitialized()) {
            DesktopAWTTreeTable treeTable = toAWTComponent();
            applyColumns(treeTable);
            treeTable.updateColumnProportion();
            queueRebuild(() -> getStructureTreeModel().invalidate());
        }
        return column;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<TableColumn<E, ?>> getColumns() {
        return List.copyOf((List) myColumns);
    }

    List<DesktopTableColumnImpl<E, ?>> getColumnImpls() {
        return myColumns;
    }

    private @Nullable Object[] columnValues(@Nullable E value) {
        if (value == null) {
            return new Object[0];
        }

        List<DesktopTableColumnImpl<E, ?>> columns = List.copyOf(myColumns);
        @Nullable Object[] values = new Object[columns.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = columns.get(i).valueOf(value);
        }
        return values;
    }

    private @Nullable DesktopTableColumnImpl<E, ?> columnAt(int modelColumn) {
        int index = modelColumn - 1;
        return index < 0 || index >= myColumns.size() ? null : myColumns.get(index);
    }

    private void applyColumns(DesktopAWTTreeTable treeTable) {
        JBTable table = treeTable.getTable();
        TableColumnModel columnModel = table.getColumnModel();

        int existing = 0;
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            existing = Math.max(existing, columnModel.getColumn(i).getModelIndex());
        }
        for (int modelIndex = existing + 1; modelIndex <= myColumns.size(); modelIndex++) {
            table.addColumn(new javax.swing.table.TableColumn(modelIndex));
        }

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            javax.swing.table.TableColumn swingColumn = columnModel.getColumn(i);
            DesktopTableColumnImpl<E, ?> column = columnAt(swingColumn.getModelIndex());
            if (column != null) {
                applyColumn(table, swingColumn, column);
            }
        }

        table.getTableHeader().repaint();
    }

    private void applyColumn(JBTable table, javax.swing.table.TableColumn swingColumn, DesktopTableColumnImpl<E, ?> column) {
        swingColumn.setHeaderValue(table.getModel().getColumnName(swingColumn.getModelIndex()));
        swingColumn.setResizable(column.isResizable());

        int width = column.getWidth(table);
        if (width > 0) {
            swingColumn.setMinWidth(width);
            swingColumn.setMaxWidth(width);
            swingColumn.setPreferredWidth(width);
        }
        else {
            swingColumn.setMinWidth(DEFAULT_MIN_WIDTH);
            swingColumn.setMaxWidth(Integer.MAX_VALUE);
        }

        if (!(swingColumn.getCellRenderer() instanceof DesktopTreeTableCellRenderer)) {
            swingColumn.setCellRenderer(new DesktopTreeTableCellRenderer<>(this, column));
        }
        if (!(swingColumn.getCellEditor() instanceof DesktopTreeTableCellEditor)) {
            swingColumn.setCellEditor(new DesktopTreeTableCellEditor<>(this, column));
        }
    }

    @RequiredUIAccess
    private void columnChanged(DesktopTableColumnImpl<E, ?> column) {
        if (!isInitialized()) {
            return;
        }

        DesktopAWTTreeTable treeTable = toAWTComponent();
        applyColumns(treeTable);
        treeTable.getTable().repaint();

        DesktopTreeTableRowSorter<E> sorter = myRowSorter;
        RowSorter.SortKey key = sorter == null ? null : sorter.getSortKey();
        if (sorter == null || key == null || columnAt(key.getColumn()) != column) {
            return;
        }

        Comparator<?> valueComparator = column.getValueComparator();
        if (valueComparator == null) {
            sorter.setSortKeys(List.of());
        }
        else if (valueComparator != mySortValueComparator) {
            sortChanged(key);
        }
    }

    boolean isSortable(int modelColumn) {
        DesktopTableColumnImpl<E, ?> column = columnAt(modelColumn);
        return column != null && column.getValueComparator() != null;
    }

    @SuppressWarnings("rawtypes")
    void sortChanged(RowSorter.@Nullable SortKey key) {
        DesktopTableColumnImpl<E, ?> column = key == null ? null : columnAt(key.getColumn());
        Comparator<NodeDescriptor> comparator = column == null || key == null
            ? null
            : comparator(column, key.getColumn() - 1, key.getSortOrder() != SortOrder.DESCENDING);

        mySortValueComparator = comparator == null || column == null ? null : column.getValueComparator();
        queueRebuild(() -> getStructureTreeModel().setComparator(comparator));
    }

    @SuppressWarnings("rawtypes")
    private static <T, V> @Nullable Comparator<NodeDescriptor> comparator(DesktopTableColumnImpl<T, V> column,
                                                                          int index,
                                                                          boolean ascending) {
        Comparator<V> valueComparator = column.getValueComparator();
        if (valueComparator == null) {
            return null;
        }

        Comparator<@Nullable V> nullsFirst = Comparator.nullsFirst(valueComparator);
        Comparator<NodeDescriptor> byValue = (left, right) -> nullsFirst.compare(
            DesktopTreeTableImpl.<V>columnValue(left, index),
            DesktopTreeTableImpl.<V>columnValue(right, index)
        );
        return ascending ? byValue : byValue.reversed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <V> @Nullable V columnValue(NodeDescriptor descriptor, int index) {
        return descriptor instanceof DesktopTreeNodeDescriptor<?> nodeDescriptor ? (V) nodeDescriptor.getColumnValue(index) : null;
    }

    private CompletableFuture<?> queueRebuild(Supplier<? extends Promise<?>> action) {
        return queue(() -> rebuildKeepingState(action));
    }

    private CompletableFuture<?> queue(Supplier<? extends CompletableFuture<?>> step) {
        CompletableFuture<@Nullable Object> next = new CompletableFuture<>();
        CompletableFuture<?> previous;
        synchronized (myRebuildLock) {
            previous = myRebuild;
            myRebuild = next;
        }

        previous.whenComplete((result, error) -> getUIAccess().giveIfNeed(() -> runQueued(step, next)));
        return next;
    }

    @RequiredUIAccess
    private void runQueued(Supplier<? extends CompletableFuture<?>> step, CompletableFuture<@Nullable Object> next) {
        if (Disposer.isDisposed(destroyHook())) {
            next.complete(null);
            return;
        }

        CompletableFuture<?> done;
        try {
            done = step.get();
        }
        catch (ProcessCanceledException e) {
            next.complete(null);
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            next.complete(null);
            return;
        }

        done.whenComplete((ignored, error) -> {
            Throwable failure = failureOf(error);
            if (failure != null) {
                LOG.error(failure);
            }
            next.complete(null);
        });
    }

    private static @Nullable Throwable failureOf(@Nullable Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause instanceof CancellationException || cause instanceof ProcessCanceledException ? null : cause;
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        DesktopTreeTableRowSorter<E> sorter = myRowSorter;
        if (sorter == null || sorter.getSortKey() == null || !(node instanceof DesktopTreeNodeImpl<E> impl)) {
            super.refreshItem(node, refreshChildren);
            return;
        }

        if (refreshChildren) {
            impl.outdateChildren();
        }

        DesktopTreeNodeImpl<E> parent = impl.getParent();
        boolean queued;
        synchronized (myRebuildLock) {
            queued = !myPendingLevels.isEmpty();
            myPendingLevels.add(parent == null ? impl : parent);
        }

        if (!queued) {
            queue(this::refreshPendingLevels);
        }
    }

    @RequiredUIAccess
    private CompletableFuture<?> refreshPendingLevels() {
        Set<DesktopTreeNodeImpl<E>> levels;
        synchronized (myRebuildLock) {
            levels = new LinkedHashSet<>(myPendingLevels);
            myPendingLevels.clear();
        }

        return levels.isEmpty() ? CompletableFuture.completedFuture(null) : refreshLevelsKeepingState(levels);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return queueRebuild(() -> {
            getStructure().getRootNode().outdateChildren();

            return getStructureTreeModel().invalidate();
        });
    }

    @Nullable
    E valueAtRow(int row) {
        TreePath path = getTree().getPathForRow(row);
        TreeNode<E> node = path == null ? null : nodeOf(path);
        return node == null ? null : node.getValue();
    }

    boolean onTableDoubleClick(MouseEvent event, int row) {
        TreePath path = getTree().getPathForRow(row);
        TreeNode<E> node = path == null ? null : nodeOf(path);
        if (node == null) {
            return false;
        }

        return fireDoubleClick(node, DesktopAWTInputDetails.convert(toAWTComponent(), event));
    }

    @Override
    public void addStyle(TreeStyle style) {
        DesktopAWTTreeTable treeTable = toAWTComponent();
        applyStyle(treeTable.getTree(), style);
        applyStyle(treeTable.getTable(), style);

        if (style != TreeStyle.TRANSPARENT_BACKGROUND) {
            return;
        }

        treeTable.setOpaque(false);
        for (java.awt.Component component : getInputComponents(treeTable)) {
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, component) instanceof JScrollPane scrollPane) {
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);
            }
        }
    }

    @Override
    public void focus() {
        getTree().requestFocus();
    }

    @Override
    public boolean isFocusable() {
        return getTree().isFocusable();
    }

    @Override
    public void setFocusable(boolean focusable) {
        DesktopAWTTreeTable treeTable = toAWTComponent();
        treeTable.getTree().setFocusable(focusable);
        treeTable.getTable().setFocusable(focusable);
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);

        DesktopAWTTreeTable treeTable = toAWTComponent();
        treeTable.getTree().setEnabled(value);
        treeTable.getTable().setEnabled(value);
    }
}
