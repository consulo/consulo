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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TableColumn;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeTable;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeTableColumns;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.widgets.QHeaderView;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtTreeTableImpl<E> extends DesktopQtTreeImpl<E> implements TreeTable<E>, TableColumnOwner {
    private final TreeTableColumns<E> myColumns = new TreeTableColumns<>();
    private final Map<Integer, Integer> myUserWidths = new HashMap<>();

    private LocalizeValue myTreeColumnHeader = LocalizeValue.empty();

    private @Nullable QTimer myFitTimer;

    public DesktopQtTreeTableImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        myController.setColumnValueFactory(myColumns::values);
    }

    @Override
    protected QTreeWidget createQt(QWidget parent) {
        QTreeWidget tree = super.createQt(parent);
        tree.setHeaderHidden(false);
        tree.setAllColumnsShowFocus(true);

        QHeaderView header = tree.header();
        header.setSectionsMovable(false);
        header.setStretchLastSection(false);
        header.setSortIndicatorShown(true);
        header.setSortIndicatorClearable(true);
        return tree;
    }

    @Override
    protected void initialize(QTreeWidget tree) {
        QTimer fitTimer = new QTimer(tree);
        fitTimer.setSingleShot(true);
        fitTimer.setInterval(0);
        fitTimer.timeout.connect(this::fitColumns);
        myFitTimer = fitTimer;

        applyHeader(tree);
        applyLayout(tree);
        applySortState(tree);

        QHeaderView header = tree.header();
        header.sectionClicked.connect(this::onSectionClicked);
        header.sectionResized.connect((section, oldSize, newSize) -> onSectionResized(header, section, newSize));
        tree.itemExpanded.connect(item -> requestFit());

        super.initialize(tree);
    }

    @Override
    public void setTreeColumnHeader(LocalizeValue header) {
        myTreeColumnHeader = header;

        headerChanged();
    }

    @Override
    public <V> TableColumn<E, V> addColumn(LocalizeValue header, Function<E, V> valueProvider) {
        TableColumnImpl<E, V> column = myColumns.add(this, header, valueProvider);

        QTreeWidget tree = boundTree();
        if (tree != null) {
            applyHeader(tree);
            applyLayout(tree);
            applySortState(tree);
        }

        myController.refreshColumnValues();
        return column;
    }

    @Override
    public List<TableColumn<E, ?>> getColumns() {
        return myColumns.getTableColumns();
    }

    @Override
    public void headerChanged() {
        QTreeWidget tree = boundTree();
        if (tree != null) {
            applyHeader(tree);
        }
    }

    @Override
    public void renderChanged() {
        QTreeWidget tree = boundTree();
        if (tree != null) {
            applyHeader(tree);
            renderAll();
        }
    }

    @Override
    public void layoutChanged() {
        QTreeWidget tree = boundTree();
        if (tree != null) {
            applyLayout(tree);
        }
    }

    @Override
    public void sortChanged() {
        Comparator<TreeNodeImpl<E>> comparator = myColumns.getSortComparator();
        if (comparator != null || myController.getSortComparator() != null) {
            myController.setSortComparator(comparator);
        }

        QTreeWidget tree = boundTree();
        if (tree != null) {
            applySortState(tree);
        }
    }

    @Override
    protected void render(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        super.render(node, item);

        int computed = node.getColumnValues().size();
        int count = myColumns.size();
        for (int index = 0; index < count; index++) {
            int section = index + 1;

            DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
            if (index < computed) {
                myColumns.render(index, presentation, node, false);
            }

            item.setText(section, presentation.toString());
            DesktopQtTextItemDelegate.bind(item, section, presentation);
            item.setIcon(section, DesktopQtImage.toQIcon(presentation.getImage()));
            item.setBackground(section, DesktopQtTextItemDelegate.toBrush(presentation.getBackgroundColor()));
            item.setTextAlignment(section, DesktopQtColumnSupport.toAlignment(myColumns.get(index).getAlignment()));
        }

        requestFit();
    }

    private @Nullable QTreeWidget boundTree() {
        QTreeWidget tree = myComponent;
        return tree == null || tree.isDisposed() ? null : tree;
    }

    private void applyHeader(QTreeWidget tree) {
        List<TableColumnImpl<E, ?>> columns = myColumns.getColumns();

        List<String> labels = new ArrayList<>(columns.size() + 1);
        labels.add(myTreeColumnHeader.get());
        for (TableColumnImpl<E, ?> column : columns) {
            labels.add(column.getHeader().get());
        }

        sync(() -> {
            tree.setColumnCount(labels.size());
            tree.setHeaderLabels(labels);

            QTreeWidgetItem headerItem = tree.headerItem();
            headerItem.setTextAlignment(0, DesktopQtColumnSupport.toAlignment(HorizontalAlignment.LEFT));
            for (TableColumnImpl<E, ?> column : columns) {
                headerItem.setTextAlignment(column.getIndex() + 1, DesktopQtColumnSupport.toAlignment(column.getAlignment()));
            }
        });
    }

    private void applyLayout(QTreeWidget tree) {
        QHeaderView header = tree.header();
        sync(() -> {
            header.setTextElideMode(Qt.TextElideMode.ElideRight);
            header.setSectionResizeMode(0, QHeaderView.ResizeMode.Stretch);

            for (TableColumnImpl<E, ?> column : myColumns.getColumns()) {
                int section = column.getIndex() + 1;
                if (section < header.count()) {
                    DesktopQtColumnSupport.applyWidth(header, section, column, preferredWidth(tree, section, column));
                }
            }
        });
    }

    private int preferredWidth(QTreeWidget tree, int section, TableColumnImpl<E, ?> column) {
        Integer userWidth = column.isResizable() ? myUserWidths.get(section) : null;
        if (userWidth != null) {
            return userWidth;
        }

        return column.getWidth() > 0 ? DesktopQtColumnSupport.explicitWidth(tree.header(), section, column) : contentWidth(tree, section);
    }

    private boolean isFittedToContent(TableColumnImpl<E, ?> column, int section) {
        return column.getWidth() <= 0 && !(column.isResizable() && myUserWidths.containsKey(section));
    }

    private static int contentWidth(QTreeWidget tree, int section) {
        return Math.max(tree.header().sectionSizeHint(section), tree.sizeHintForColumn(section));
    }

    private void requestFit() {
        QTimer timer = myFitTimer;
        if (timer != null && !timer.isDisposed() && !timer.isActive()) {
            timer.start();
        }
    }

    private void fitColumns() {
        QTreeWidget tree = boundTree();
        if (tree == null) {
            return;
        }

        QHeaderView header = tree.header();
        sync(() -> {
            for (TableColumnImpl<E, ?> column : myColumns.getColumns()) {
                int section = column.getIndex() + 1;
                if (section < header.count() && isFittedToContent(column, section)) {
                    int width = contentWidth(tree, section);
                    if (width > header.sectionSize(section)) {
                        header.resizeSection(section, width);
                    }
                }
            }
        });
    }

    private void applySortState(QTreeWidget tree) {
        QHeaderView header = tree.header();
        boolean sortable = myColumns.getColumns().stream().anyMatch(TableColumnImpl::isSortable);
        int column = myColumns.getSortColumn();
        Qt.SortOrder order = myColumns.isSortAscending() ? Qt.SortOrder.AscendingOrder : Qt.SortOrder.DescendingOrder;

        sync(() -> {
            header.setSectionsClickable(sortable);
            header.setSortIndicator(column < 0 ? -1 : column + 1, order);
        });
    }

    private void onSectionClicked(int section) {
        int column = section - 1;
        if (column >= 0 && column < myColumns.size() && myColumns.get(column).isSortable()) {
            myController.setSortComparator(myColumns.cycleSort(column));
        }

        QTreeWidget tree = boundTree();
        if (tree != null) {
            applySortState(tree);
        }
    }

    private void onSectionResized(QHeaderView header, int section, int newSize) {
        if (isSyncing() || section < 1 || header.isDisposed()) {
            return;
        }

        if (header.sectionResizeMode(section) == QHeaderView.ResizeMode.Interactive) {
            myUserWidths.put(section, newSize);
        }
    }
}
