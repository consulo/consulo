/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.util.collection;

import consulo.util.lang.function.Functions;
import consulo.util.lang.function.Predicates;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static consulo.util.lang.function.Predicates.not;

/**
 * A redesigned version of com.google.common.collect.TreeTraversal.
 * <p/>
 * The original JavaDoc:
 * <p/>
 * Views elements of a type {@code T} as nodes in a tree, and provides methods to traverse the trees
 * induced by this traverser.
 * <p/>
 * <p>For example, the tree
 * <p/>
 * <pre>{@code
 *          h
 *        / | \
 *       /  e  \
 *      d       g
 *     /|\      |
 *    / | \     f
 *   a  b  c       }</pre>
 * <p/>
 * <p>can be iterated over in pre-order (hdabcegf), post-order (abcdefgh), or breadth-first order
 * (hdegabcf).
 * <p/>
 * <p>Null nodes are strictly forbidden.
 *
 * @author Louis Wasserman
 * <p/>
 * @author gregsh
 */
public abstract class TreeTraversal {
    private final String debugName;

    protected TreeTraversal(String debugName) {
        this.debugName = debugName;
    }

    public final <T> JBIterable<T> traversal(
        Iterable<? extends T> roots,
        Function<T, ? extends Iterable<? extends T>> tree
    ) {
        return new JBIterable<>() {
            @Override
            public Iterator<T> iterator() {
                return createIterator(roots, tree);
            }
        };
    }

    public final <T> JBIterable<T> traversal(@Nullable T root, Function<T, ? extends Iterable<? extends T>> tree) {
        return traversal(root == null ? List.of() : List.of(root), tree);
    }

    public final <T> Function<T, JBIterable<T>> traversal(Function<T, ? extends Iterable<? extends T>> tree) {
        return t -> traversal(t, tree);
    }

    /**
     * Configures the traversal to skip already visited nodes.
     *
     * @see TreeTraversal#unique(Function)
     */
    public final TreeTraversal unique() {
        return unique(Function.identity());
    }

    /**
     * Configures the traversal to skip already visited nodes.
     *
     * @param identity function
     */
    public TreeTraversal unique(Function<?, ?> identity) {
        TreeTraversal original = this;
        return new TreeTraversal(debugName + " (UNIQUE)") {
            @Override
            public <T> It<T> createIterator(
                Iterable<? extends T> roots,
                Function<T, ? extends Iterable<? extends T>> tree
            ) {
                class WrappedTree implements Predicate<T>, Function<T, Iterable<? extends T>> {
                    @Nullable Set<Object> visited;

                    @Override
                    public boolean test(T e) {
                        if (visited == null) {
                            visited = new HashSet<>();
                        }
                        //noinspection unchecked
                        return visited.add(((Function<T, Object>)identity).apply(e));
                    }

                    @Override
                    public Iterable<? extends T> apply(T t) {
                        return JBIterable.from(tree.apply(t)).filter(this);
                    }
                }
                if (tree instanceof WrappedTree) {
                    return original.createIterator(roots, tree);
                }
                WrappedTree wrappedTree = new WrappedTree();
                return original.createIterator(JBIterable.from(roots).filter(wrappedTree), wrappedTree);
            }
        };
    }

    /**
     * Configures the traversal to expand and return the nodes within the range only.
     * It is an optimized version of expand-and-filter operation.
     * It skips all the nodes "before" the {@code rangeCondition} return true for the first time,
     * processes as usual the nodes while the condition return true and
     * stops when the {@code rangeCondition} return false after that.
     */
    public TreeTraversal onRange(Predicate<?> rangeCondition) {
        TreeTraversal original = this;
        return new TreeTraversal(original.toString() + " (ON_RANGE)") {
            @Override
            @SuppressWarnings("unchecked")
            public <T> It<T> createIterator(
                Iterable<? extends T> roots,
                Function<T, ? extends Iterable<? extends T>> tree
            ) {
                Predicate<? super T> inRangeCondition = (Predicate<? super T>)rangeCondition;
                Predicate<? super T> notInRangeCondition = (Predicate<? super T>)not(rangeCondition);
                class WrappedTree implements Function<T, Iterable<? extends T>> {
                    @Override
                    public Iterable<? extends T> apply(T t) {
                        return JBIterable.from(tree.apply(t)).skipWhile(notInRangeCondition).takeWhile(inRangeCondition);
                    }
                }
                if (tree instanceof WrappedTree) {
                    return original.createIterator(roots, tree);
                }
                WrappedTree wrappedTree = new WrappedTree();
                return original.createIterator(JBIterable.from(roots).filter(inRangeCondition), wrappedTree);
            }
        };
    }

    /**
     * Creates a new iterator for this type of traversal.
     *
     * @param roots tree roots
     * @param tree  tree structure the children for parent function.
     *              May return null (useful for map representation).
     */
    public abstract <T> It<T> createIterator(
        Iterable<? extends T> roots,
        Function<T, ? extends Iterable<? extends T>> tree
    );

    @Override
    public final String toString() {
        return debugName;
    }

    public static abstract class It<T> extends JBIterator<T> {
        protected final Function<? super T, ? extends Iterable<? extends T>> tree;

        protected It(Function<? super T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
        }
    }

    public static abstract class TracingIt<T> extends It<T> {

        public @Nullable T parent() {
            throw new UnsupportedOperationException();
        }

        public JBIterable<T> backtrace() {
            throw new UnsupportedOperationException();
        }

        protected TracingIt(Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
        }

        protected JBIterable<T> _transform(JBIterable<?> original) {
            JBIterable<?> result = original;
            for (Function<Object, Object> f : getTransformations()) {
                result = result.transform(f);
            }
            //noinspection unchecked
            return (JBIterable<T>)result;
        }

        protected @Nullable T _transform(@Nullable Object original) {
            Object result = original;
            for (Function<Object, ?> f : getTransformations()) {
                result = f.apply(result);
            }
            //noinspection unchecked
            return (T)result;
        }
    }

    public static abstract class GuidedIt<T> extends It<T> {

        public interface Guide<T> {
            void guide(GuidedIt<T> guidedIt);
        }

        public @Nullable T curChild, curParent;
        public @Nullable Iterable<? extends T> curChildren;
        public boolean curNoChildren;

        public abstract GuidedIt<T> queueNext(@Nullable T child);

        public abstract GuidedIt<T> result(@Nullable T node);

        public abstract GuidedIt<T> queueLast(@Nullable T child);

        protected GuidedIt(Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
        }
    }

    public static TreeTraversal GUIDED_TRAVERSAL(GuidedIt.Guide<?> guide) {
        return new TreeTraversal("GUIDED_TRAVERSAL") {
            @Override
            public <T> It<T> createIterator(
                Iterable<? extends T> roots,
                Function<T, ? extends Iterable<? extends T>> tree
            ) {
                //noinspection unchecked
                return new GuidedItImpl<>(roots, tree, (GuidedIt.Guide<T>)guide);
            }
        };
    }

    /**
     * Returns an iterator over the nodes in a tree structure, using pre-order traversal.
     * That is, each node's subtrees are traversed after the node itself is returned.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    public static final TreeTraversal PRE_ORDER_DFS = new TreeTraversal("PRE_ORDER_DFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new PreOrderIt<>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using post-order DFS traversal.
     * That is, each node's subtrees are traversed before the node itself is returned.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    public static final TreeTraversal POST_ORDER_DFS = new TreeTraversal("POST_ORDER_DFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new PostOrderIt<>(roots, tree);
        }
    };


    /**
     * Returns an iterator over the leaf nodes only in a tree structure, using DFS traversal.
     * That is, each node's subtrees are traversed before the node itself is returned.
     */
    public static final TreeTraversal LEAVES_DFS = new TreeTraversal("LEAVES_DFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new LeavesDfsIt<>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using interlaced pre-order DFS traversal.
     * That is, all paths are traversed in an interlaced manner that is suitable for infinite and cyclic graphs
     * and each node's subtrees are traversed before the node itself is returned.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    public static final TreeTraversal INTERLEAVED_DFS = new TreeTraversal("INTERLEAVED_DFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new InterleavedIt<>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using breadth-first traversal.
     * That is, all the nodes of depth 0 are returned, then depth 1, then 2, and so on.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    public static final TreeTraversal PLAIN_BFS = new TreeTraversal("PLAIN_BFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new PlainBfsIt<>(roots, tree);
        }
    };

    /**
     * Same as {@code PLAIN_BFS} but with {@code TracingIt}.
     * That is, a path to the current node can be retrieved during some traversal.
     *
     * @see TreeTraversal.TracingIt
     */
    public static final TreeTraversal TRACING_BFS = new TreeTraversal("TRACING_BFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new TracingBfsIt<>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the leaf nodes only in a tree structure, using BFS traversal.
     * That is, all the leaves of depth 0 are returned, then depth 1, then 2, and so on.
     */
    public static final TreeTraversal LEAVES_BFS = new TreeTraversal("LEAVES_BFS") {
        @Override
        public <T> It<T> createIterator(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            return new LeavesBfsIt<>(roots, tree);
        }
    };

    // -----------------------------------------------------------------------------
    // Iterators: DFS
    // -----------------------------------------------------------------------------

    private abstract static class DfsIt<T, H extends P<T, H>> extends TracingIt<T> {
        @Nullable H last = null;

        protected DfsIt(Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
        }

        @Override
        public @Nullable T parent() {
            if (last == null) {
                throw new NoSuchElementException();
            }

            H p = last.parent;
            return p == null ? null : p.node == null ? null : _transform(p.node);
        }

        @Override
        public JBIterable<T> backtrace() {
            if (last == null) {
                throw new NoSuchElementException();
            }
            return _transform(JBIterable.generate(last, P.<T>toPrev()).transform(P.<T>toNode()).filter(Predicates.notNull()));
        }
    }

    private final static class PreOrderIt<T> extends DfsIt<T, P1<T>> {
        PreOrderIt(Iterable<? extends T> roots, Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            last = P1.create(roots);
        }

        @Override
        public @Nullable T nextImpl() {
            while (last != null) {
                Iterator<? extends T> it = last.iterator(tree);
                if (it.hasNext()) {
                    T result = it.next();
                    last = last.add(P1.create(result));
                    return result;
                }
                else {
                    last = last.remove();
                }
            }
            return stop();
        }
    }

    private static final class PostOrderIt<T> extends DfsIt<T, P1<T>> {
        PostOrderIt(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            for (T root : roots) {
                P1<T> p = P1.create(root);
                last = last == null ? p : last.add(p);
            }
        }

        @Override
        public @Nullable T nextImpl() {
            while (last != null) {
                Iterator<? extends T> it = last.iterator(tree);
                if (it.hasNext()) {
                    T result = it.next();
                    last = last.add(P1.create(result));
                }
                else {
                    T result = last.node;
                    last = last.remove();
                    return result;
                }
            }
            return stop();
        }
    }

    private final static class LeavesDfsIt<T> extends DfsIt<T, P1<T>> {
        LeavesDfsIt(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            last = P1.create(roots);
        }

        @Override
        public @Nullable T nextImpl() {
            while (last != null) {
                P1<T> top = last;
                if (top.iterator(tree).hasNext() && !top.empty) {
                    T child = top.iterator(tree).next();
                    last = last.add(P1.create(child));
                }
                else {
                    last = last.remove();
                    if (top.empty) {
                        return last == null ? stop() : top.node;
                    }
                }
            }
            return stop();
        }
    }

    private final static class InterleavedIt<T> extends DfsIt<T, P2<T>> {
        @Nullable P2<T> cur, max;

        InterleavedIt(Iterable<? extends T> roots, Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            last = P2.create(roots);
            cur = max = last;
        }

        @Override
        public @Nullable T nextImpl() {
            while (last != null) {
                if (cur == null) {
                    cur = max;
                    max = Objects.requireNonNull(max).next;
                }
                Iterator<? extends T> it = Objects.requireNonNull(cur).iterator(tree);
                if (it.hasNext()) {
                    T result = it.next();
                    last = last.add(P2.create(result));
                    last.parent = cur;
                    cur = cur.prev;
                    if (max == null) {
                        max = last;
                    }
                    return result;
                }
                else {
                    if (cur == last) {
                        last = cur.prev;
                    }
                    cur = cur.remove();
                }
            }
            return stop();
        }
    }

    // -----------------------------------------------------------------------------
    // Iterators: BFS
    // -----------------------------------------------------------------------------

    private static final class PlainBfsIt<T> extends It<T> {
        final ArrayDeque<T> queue = new ArrayDeque<>();

        @Nullable P1<T> top = null;

        PlainBfsIt(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public @Nullable T nextImpl() {
            if (top != null) {
                JBIterable.from(top.iterable(tree)).addAllTo(queue);
                top = null;
            }
            if (queue.isEmpty()) {
                return stop();
            }
            top = P1.create(queue.remove());
            return top.node;
        }
    }

    private static final class LeavesBfsIt<T> extends TracingIt<T> {
        final ArrayDeque<T> queue = new ArrayDeque<>();

        LeavesBfsIt(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public @Nullable T nextImpl() {
            while (!queue.isEmpty()) {
                T result = queue.remove();
                Iterable<? extends T> children = tree.apply(result);
                Iterator<? extends T> it = children == null ? null : children.iterator();
                if (it == null || !it.hasNext()) {
                    return result;
                }
                while (it.hasNext()) queue.add(it.next());
            }
            return stop();
        }
    }

    private final static class TracingBfsIt<T> extends TracingIt<T> {
        final ArrayDeque<T> queue = new ArrayDeque<>();
        final Map<T, T> paths = Maps.newHashMap(HashingStrategy.identity());

        @Nullable P1<T> top = null;

        TracingBfsIt(Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public @Nullable T nextImpl() {
            if (top != null) {
                for (T t : top.iterable(tree)) {
                    if (paths.containsKey(t)) {
                        continue;
                    }
                    queue.add(t);
                    paths.put(t, top.node);
                }
                top = null;
            }
            if (queue.isEmpty()) {
                return stop();
            }
            top = P1.create(queue.remove());
            return top.node;
        }

        @Override
        public @Nullable T parent() {
            if (top == null) {
                throw new NoSuchElementException();
            }
            return _transform(paths.get(top.node));
        }

        @Override
        public JBIterable<T> backtrace() {
            if (top == null) {
                throw new NoSuchElementException();
            }
            return _transform(JBIterable.generate(top.node, Functions.fromMap(paths)));
        }
    }

    // -----------------------------------------------------------------------------
    // Misc
    // -----------------------------------------------------------------------------
    private static final class GuidedItImpl<T> extends GuidedIt<T> {
        final Guide<T> guide;

        P1<T> first;

        @Nullable P1<T> last;

        @Nullable T curResult = null;

        GuidedItImpl(
            Iterable<? extends T> roots,
            Function<T, ? extends Iterable<? extends T>> tree,
            Guide<T> guide
        ) {
            super(tree);
            first = last = P1.create(roots);
            this.guide = guide;
        }

        @Override
        public GuidedIt<T> queueNext(@Nullable T child) {
            if (child != null) {
                last = Objects.requireNonNull(last).add(P1.create(child));
            }
            return this;
        }

        @Override
        public GuidedIt<T> queueLast(@Nullable T child) {
            if (child != null) {
                first = first.addBefore(P1.create(child));
            }
            return this;
        }

        @Override
        public GuidedIt<T> result(@Nullable T node) {
            curResult = node;
            return this;
        }

        @Override
        public @Nullable T nextImpl() {
            if (guide == null) {
                return stop();
            }
            while (last != null) {
                P<T, ?> top = last;
                Iterator<? extends T> it = top.iterator(tree);
                boolean hasNext = it.hasNext();
                curResult = null;
                if (top.node != null || hasNext) {
                    curChild = hasNext ? it.next() : null;
                    curParent = top.node;
                    curChildren = top.itle;
                    curNoChildren = top.empty;
                    guide.guide(this);
                }
                if (!hasNext) {
                    last = last.remove();
                }
                if (curResult != null) {
                    return curResult;
                }
            }
            return stop();
        }
    }

    private static class P<T, Self extends P<T, Self>> {
        @Nullable T node = null;

        @Nullable Iterable<? extends T> itle = null;

        @Nullable Iterator<? extends T> it = null;

        boolean empty;

        @Nullable Self parent;

        static <T, Self extends P<T, Self>> Self create(Self p, T node) {
            p.node = node;
            return p;
        }

        static <T, Self extends P<T, Self>> Self create(Self p, Iterable<? extends T> it) {
            p.itle = it;
            return p;
        }

        Iterator<? extends T> iterator(Function<? super T, ? extends Iterable<? extends T>> tree) {
            if (it != null) {
                return it;
            }
            it = iterable(tree).iterator();
            empty = itle == null || !it.hasNext();
            return it;
        }

        Iterable<? extends T> iterable(Function<? super T, ? extends Iterable<? extends T>> tree) {
            return itle != null ? itle : JBIterable.from(itle = tree.apply(node));
        }

        /**
         * @noinspection unchecked
         */
        static <T> Function<P<T, ?>, T> toNode() {
            return TO_NODE;
        }

        /**
         * @noinspection unchecked
         */
        static <T> Function<P<T, ?>, P<T, ?>> toPrev() {
            return TO_PREV;
        }

        static final Function TO_NODE = new Function<P<?, ?>, Object>() {
            @Override
            public @Nullable Object apply(P<?, ?> tp) {
                return tp.node;
            }
        };
        static final Function TO_PREV = new Function<P<?, ?>, P<?, ?>>() {
            @Override
            public @Nullable P<?, ?> apply(P<?, ?> tp) {
                return tp.parent;
            }
        };
    }

    private static final class P1<T> extends P<T, P1<T>> {
        static <T> P1<T> create(T node) {
            return create(new P1<>(), node);
        }

        static <T> P1<T> create(Iterable<? extends T> it) {
            return create(new P1<T>(), it);
        }

        P1<T> add(P1<T> next) {
            next.parent = this;
            return next;
        }

        P1<T> addBefore(P1<T> next) {
            next.parent = null;
            this.parent = next;
            return next;
        }

        @Nullable P1<T> remove() {
            P1<T> p = parent;
            parent = null;
            return p;
        }

        @Override
        public String toString() {
            int h = 0;
            for (P1<T> p = parent; p != null; p = p.parent) h++;
            return h + ": " + node;
        }
    }

    private static final class P2<T> extends P<T, P2<T>> {
        @Nullable P2<T> next, prev;

        static <T> P2<T> create(T node) {
            return create(new P2<>(), node);
        }

        static <T> P2<T> create(Iterable<? extends T> it) {
            return create(new P2<T>(), it);
        }

        P2<T> add(P2<T> next) {
            next.next = this.next;
            next.prev = this;
            this.next = next;
            return next;
        }

        @Nullable P2<T> remove() {
            P2<T> p = prev;
            P2<T> n = next;
            prev = next = null;
            if (p != null) {
                p.next = n;
            }
            if (n != null) {
                n.prev = p;
            }
            return p;
        }

        @Override
        public String toString() {
            int h = 0, t = 0;
            for (P2<T> p = prev; p != null; p = p.prev) h++;
            for (P2<T> p = next; p != null; p = p.next) t++;
            return h + " of " + (h + t + 1) + ": " + node;
        }
    }
}
