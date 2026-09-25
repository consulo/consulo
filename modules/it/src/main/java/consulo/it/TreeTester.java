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

import consulo.it.internal.HeadlessUIAccess;
import consulo.it.internal.ui.HeadlessTextItemPresentation;
import consulo.it.internal.ui.HeadlessTree;
import consulo.ui.Point2D;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.event.details.InputDetails;
import consulo.ui.impl.tree.TreeController;
import consulo.ui.impl.tree.TreeNodeImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class TreeTester<E> {
    private static final long TIMEOUT_SECONDS = 30;

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

    public Tree<E> getTree() {
        return myTree;
    }

    public TreeTester<E> bind() {
        myTree.setAttached(true);
        onUI(myController::bind);
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
        long deadline = deadline();
        do {
            await(myController.whenIdle(), deadline);
            await(HeadlessUIAccess.INSTANCE.giveAsync(() -> null), deadline);
        }
        while (!myController.isIdle());
        return this;
    }

    public TreeTester<E> flush() {
        await(HeadlessUIAccess.INSTANCE.giveAsync(() -> null), deadline());
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
        onUI(() -> myController.onExpanded(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userCollapse(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        onUI(() -> myController.onCollapsed(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userSelect(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        onUI(() -> myController.onSelected(node, userInput()));
        return this;
    }

    @SafeVarargs
    public final TreeTester<E> userDoubleClick(E... path) {
        TreeNodeImpl<E> node = (TreeNodeImpl<E>) node(path);
        onUI(() -> myController.onDoubleClick(node, userInput()));
        return this;
    }

    public String dump() {
        StringBuilder builder = new StringBuilder();
        dump(builder, myController.getRoot(), 0, myController.getSelected());
        return builder.toString();
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
            builder.append('\n');

            if (child.isExpanded()) {
                dump(builder, child, depth + 1, selected);
            }
        }
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

    private static void onUI(Runnable action) {
        if (HeadlessUIAccess.INSTANCE.isUIThread()) {
            action.run();
            return;
        }

        await(HeadlessUIAccess.INSTANCE.giveAsync(action), deadline());
    }

    private static long deadline() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
    }

    private static void await(CompletableFuture<?> future, long deadline) {
        if (HeadlessUIAccess.INSTANCE.isUIThread() && !future.isDone()) {
            throw new IllegalStateException("The UI thread cannot wait for the tree - it is what the tree waits for");
        }

        try {
            future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        catch (TimeoutException e) {
            throw new AssertionError("The tree did not settle in " + TIMEOUT_SECONDS + " seconds", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        catch (ExecutionException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
