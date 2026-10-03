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
package consulo.it;

import consulo.it.internal.ui.HeadlessTextItemPresentation;
import consulo.it.internal.ui.HeadlessTree;
import consulo.it.internal.ui.HeadlessTreeTable;
import consulo.localize.LocalizeValue;
import consulo.ui.Point2D;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeTable;
import consulo.ui.event.details.InputDetails;
import consulo.ui.impl.tree.TreeController;
import consulo.ui.impl.tree.TreeNodeImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class TreeTester<E> {
    private final HeadlessTree<E> myTree;
    private final TreeController<E> myController;

    private TreeTester(HeadlessTree<E> tree) {
        myTree = tree;
        myController = tree.getController();
    }

    public static <E> TreeTester<E> of(Tree<E> tree) {
        if (!(tree instanceof HeadlessTree<E> headlessTree)) {
            throw new IllegalArgumentException("Not a headless tree: " + tree);
        }
        return new TreeTester<>(headlessTree);
    }

    public static <E> TreeTester<E> create(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return of(Tree.create(rootValue, model, executor));
    }

    public static <E> TreeTester<E> createTable(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return of(TreeTable.create(rootValue, model, executor));
    }

    public Tree<E> getTree() {
        return myTree;
    }

    public TreeTable<E> getTreeTable() {
        return table();
    }

    public TreeTester<E> bind() {
        myTree.setAttached(true);
        HeadlessUIThread.run(myController::bind);
        return this;
    }

    public TreeTester<E> detach() {
        myTree.setAttached(false);
        return this;
    }

    public TreeTester<E> show() {
        return bind().settle();
    }

    public TreeTester<E> settle() {
        long deadline = HeadlessUIThread.deadline();
        do {
            HeadlessUIThread.await(myController.whenIdle(), deadline);
            HeadlessUIThread.flush(deadline);
        }
        while (!myController.isIdle());
        return this;
    }

    public TreeTester<E> flush() {
        HeadlessUIThread.flush();
        return this;
    }

    public boolean isIdle() {
        return myController.isIdle();
    }

    @SafeVarargs
    public final TreeNode<E> node(E... path) {
        TreeNodeImpl<E> node = myController.getRoot();
        for (E value : path) {
            TreeNodeImpl<E> next = null;
            for (TreeNodeImpl<E> child : node.getChildren()) {
                if (Objects.equals(child.getValue(), value)) {
                    next = child;
                    break;
                }
            }

            if (next == null) {
                throw new AssertionError("No node " + value + " below " + node.getValue() + " in\n" + dump());
            }
            node = next;
        }
        return node;
    }

    @SafeVarargs
    public final TreeTester<E> userExpand(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        HeadlessUIThread.run(() -> myController.onExpanded(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userCollapse(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        HeadlessUIThread.run(() -> myController.onCollapsed(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userSelect(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        HeadlessUIThread.run(() -> myController.onSelected(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userDoubleClick(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        HeadlessUIThread.run(() -> myController.onDoubleClick(node, userInput()));
        return this;
    }

    public TreeTester<E> userClickHeader(int columnIndex) {
        HeadlessTreeTable<E> table = table();
        HeadlessUIThread.run(() -> table.clickHeader(columnIndex));
        return this;
    }

    public TreeTester<E> sortBy(int columnIndex, boolean ascending) {
        HeadlessTreeTable<E> table = table();
        HeadlessUIThread.run(() -> table.sortBy(columnIndex, ascending));
        return this;
    }

    public TreeTester<E> clearSort() {
        HeadlessTreeTable<E> table = table();
        HeadlessUIThread.run(table::clearSort);
        return this;
    }

    public int getSortColumn() {
        return table().getTreeTableColumns().getSortColumn();
    }

    public boolean isSortAscending() {
        return table().getTreeTableColumns().isSortAscending();
    }

    public List<String> headers() {
        return table().getHeaders().stream().map(LocalizeValue::get).toList();
    }

    public String dump() {
        return HeadlessUIThread.compute(() -> {
            StringBuilder builder = new StringBuilder();
            dump(builder, myController.getRoot(), 0, myController.getSelected());
            return builder.toString();
        });
    }

    private void dump(StringBuilder builder, TreeNodeImpl<E> node, int depth, @Nullable TreeNodeImpl<E> selected) {
        for (TreeNodeImpl<E> child : node.getChildren()) {
            builder.append(" ".repeat(depth));
            if (!child.isLeaf()) {
                builder.append(child.isExpanded() ? '-' : '+');
            }

            boolean isSelected = child == selected;
            if (isSelected) {
                builder.append('[');
            }
            builder.append(textOf(child));
            if (isSelected) {
                builder.append(']');
            }
            if (myTree instanceof HeadlessTreeTable<E> table) {
                appendCells(builder, table, child, isSelected);
            }
            builder.append('\n');

            if (child.isExpanded()) {
                dump(builder, child, depth + 1, selected);
            }
        }
    }

    private static <T> void appendCells(StringBuilder builder, HeadlessTreeTable<T> table, TreeNodeImpl<T> node, boolean selected) {
        int columns = table.getTreeTableColumns().size();
        for (int i = 0; i < columns; i++) {
            String cell = table.renderCell(node, i, selected);
            builder.append(" |");
            if (!cell.isEmpty()) {
                builder.append(' ').append(cell);
            }
        }
    }

    private HeadlessTreeTable<E> table() {
        if (!(myTree instanceof HeadlessTreeTable<E> table)) {
            throw new IllegalStateException("Not a tree table: " + myTree);
        }
        return table;
    }

    public void assertStructure(String expected) {
        settle();

        Assertions.assertEquals(expected, dump());
    }

    private static String textOf(TreeNodeImpl<?> node) {
        return node.getPresentation() instanceof HeadlessTextItemPresentation presentation
            ? presentation.getText()
            : String.valueOf(node.getValue());
    }

    private static InputDetails userInput() {
        return new InputDetails(new Point2D(0, 0), new Point2D(0, 0));
    }
}
