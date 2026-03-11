/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class FilteredTraverserBase<T, Self extends FilteredTraverserBase<T, Self>> implements Iterable<T> {
    private final Meta<T> myMeta;
    private final Function<T, ? extends Iterable<? extends T>> myTree;

    protected FilteredTraverserBase(@Nullable Meta<T> meta, Function<T, ? extends Iterable<? extends T>> tree) {
        this.myTree = tree;
        this.myMeta = meta == null ? Meta.<T>empty() : meta;
    }

    public Function<T, ? extends Iterable<? extends T>> getTree() {
        return myTree;
    }

    public final T getRoot() {
        return myMeta.roots.iterator().next();
    }

    public final Iterable<? extends T> getRoots() {
        return myMeta.roots;
    }

    @Override
    public final Iterator<T> iterator() {
        return traverse().iterator();
    }

    protected abstract Self newInstance(Meta<T> meta);

    public final JBIterable<T> traverse(TreeTraversal traversal) {
        Function<T, Iterable<? extends T>> adjusted = this::children;
        return myMeta.interceptor.apply(traversal).traversal(getRoots(), adjusted).filter(myMeta.filter.AND);
    }

    public final JBIterable<T> traverse() {
        return traverse(myMeta.traversal);
    }

    public final JBIterable<T> preOrderDfsTraversal() {
        return traverse(TreeTraversal.PRE_ORDER_DFS);
    }

    public final JBIterable<T> postOrderDfsTraversal() {
        return traverse(TreeTraversal.POST_ORDER_DFS);
    }

    public final JBIterable<T> bfsTraversal() {
        return traverse(TreeTraversal.PLAIN_BFS);
    }

    public final JBIterable<T> tracingBfsTraversal() {
        return traverse(TreeTraversal.TRACING_BFS);
    }

    /**
     * Clears expand, regard and filter conditions, traversal while keeping roots and "forced" properties.
     *
     * @see FilteredTraverserBase#forceIgnore(Predicate)
     * @see FilteredTraverserBase#forceDisregard(Predicate)
     */
    public final Self reset() {
        return newInstance(myMeta.reset());
    }

    public final Self withRoot(@Nullable T root) {
        return newInstance(myMeta.withRoots(root == null ? List.of() : List.of(root)));
    }

    public final Self withRoots(Iterable<? extends T> roots) {
        return newInstance(myMeta.withRoots(roots));
    }

    public final Self withTraversal(TreeTraversal type) {
        return newInstance(myMeta.withTraversal(type));
    }

    /**
     * Restricts the nodes that can have children by the specified condition.
     * Subsequent calls will AND all the conditions.
     */
    public final Self expand(Predicate<? super T> c) {
        return newInstance(myMeta.expand(c));
    }

    /**
     * Restricts the nodes that can be children by the specified condition while <b>keeping the edges</b>.
     * Subsequent calls will AND all the conditions.
     */
    public final Self regard(Predicate<? super T> c) {
        return newInstance(myMeta.regard(c));
    }

    public final Self expandAndFilter(Predicate<? super T> c) {
        return newInstance(myMeta.expand(c).filter(c));
    }

    public final Self expandAndSkip(Predicate<? super T> c) {
        return newInstance(myMeta.expand(c).filter(Predicates.not(c)));
    }

    public final Self filter(Predicate<? super T> c) {
        return newInstance(myMeta.filter(c));
    }

    public final <C> JBIterable<C> filter(Class<C> type) {
        return traverse().filter(type);
    }

    /**
     * Configures the traverser to skip already visited nodes.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see FilteredTraverserBase#unique(Function)
     * @see TreeTraversal#unique()
     */
    public final Self unique() {
        return unique(Functions.identity());
    }

    /**
     * Configures the traverser to skip already visited nodes.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see TreeTraversal#unique(Function)
     */
    public final Self unique(Function<? super T, Object> identity) {
        return interceptTraversal(traversal -> traversal.unique(identity));
    }

    /**
     * Configures the traverser to expand and return the nodes within the range only.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see TreeTraversal#onRange(Predicate)
     */
    public Self onRange(Predicate<? super T> rangeCondition) {
        return interceptTraversal(traversal -> traversal.onRange(rangeCondition));
    }

    /**
     * Excludes the nodes by the specified condition from any traversal completely.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see FilteredTraverserBase#expand(Predicate)
     * @see FilteredTraverserBase#filter(Predicate)
     * @see FilteredTraverserBase#reset()
     */
    public final Self forceIgnore(Predicate<? super T> c) {
        return newInstance(myMeta.forceIgnore(c));
    }

    /**
     * Excludes the nodes by the specified condition while keeping their edges.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see FilteredTraverserBase#regard(Predicate)
     * @see FilteredTraverserBase#reset()
     */
    public final Self forceDisregard(Predicate<? super T> c) {
        return newInstance(myMeta.forceDisregard(c));
    }

    /**
     * Intercepts and alters traversal just before the walking.
     * <p/>
     * This property is not reset by {@code reset()} call.
     *
     * @see FilteredTraverserBase#unique()
     * @see FilteredTraverserBase#onRange(Predicate)
     */
    public final Self interceptTraversal(Function<TreeTraversal, TreeTraversal> transform) {
        return newInstance(myMeta.interceptTraversal(transform));
    }

    /**
     * Returns the children of the specified node as seen by this traverser.
     *
     * @see TreeTraversal.TracingIt to obtain parents of a node during a traversal
     */
    public final JBIterable<T> children(@Nullable T node) {
        if (node == null || isAlwaysLeaf(node)) {
            return JBIterable.empty();
        }
        else if (myMeta.regard.next == null && myMeta.forceDisregard.next == null) {
            return JBIterable.<T>from(myTree.apply(node)).filter(Predicates.not(myMeta.forceIgnore.OR));
        }
        else {
            // traverse subtree to select accepted children
            return TreeTraversal.GUIDED_TRAVERSAL(myMeta.createChildrenGuide(node)).traversal(node, myTree);
        }
    }

    protected boolean isAlwaysLeaf(T node) {
        return !myMeta.expand.valueAnd(node);
    }

    public final List<T> toList() {
        return traverse().toList();
    }

    public final Set<T> toSet() {
        return traverse().toSet();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "traversal=" + myMeta.traversal +
            '}';
    }

    public abstract static class EdgeFilter<T> extends JBIterable.SCond<T> {
        @Nullable
        protected T edgeSource = null;
    }

    @SuppressWarnings("unchecked")
    protected static class Meta<T> {
        final TreeTraversal traversal;

        final Iterable<? extends T> roots;
        final Cond<T> expand;
        final Cond<T> regard;
        final Cond<T> filter;

        final Cond<T> forceIgnore;
        final Cond<T> forceDisregard;

        final Function<TreeTraversal, TreeTraversal> interceptor;

        public Meta(
            Iterable<? extends T> roots,
            TreeTraversal traversal,
            Cond<T> expand,
            Cond<T> regard,
            Cond<T> filter,
            Cond<T> forceIgnore,
            Cond<T> forceDisregard,
            Function<TreeTraversal, TreeTraversal> interceptor
        ) {
            this.roots = roots;
            this.traversal = traversal;
            this.expand = expand;
            this.regard = regard;
            this.filter = filter;
            this.forceIgnore = forceIgnore;
            this.forceDisregard = forceDisregard;
            this.interceptor = interceptor;
        }

        public Meta<T> reset() {
            Meta<T> e = empty();
            return new Meta<>(roots, e.traversal, e.expand, e.regard, e.filter, forceIgnore, forceDisregard, e.interceptor);
        }

        public Meta<T> withRoots(Iterable<? extends T> roots) {
            return new Meta<>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard, interceptor);
        }

        public Meta<T> withTraversal(TreeTraversal traversal) {
            return new Meta<>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard, interceptor);
        }

        public Meta<T> expand(Predicate<? super T> c) {
            return new Meta<>(roots, traversal, expand.append(c), regard, this.filter, forceIgnore, forceDisregard, interceptor);
        }

        public Meta<T> regard(Predicate<? super T> c) {
            return new Meta<>(roots, traversal, expand, regard.append(c), this.filter, forceIgnore, forceDisregard, interceptor);
        }

        public Meta<T> filter(Predicate<? super T> c) {
            return new Meta<>(roots, traversal, expand, regard, this.filter.append(c), forceIgnore, forceDisregard, interceptor);
        }

        public Meta<T> forceIgnore(Predicate<? super T> c) {
            return new Meta<>(roots, traversal, expand, regard, this.filter, forceIgnore.append(c), forceDisregard, interceptor);
        }

        public Meta<T> forceDisregard(Predicate<? super T> c) {
            return new Meta<>(roots, traversal, expand, regard, this.filter, forceIgnore, forceDisregard.append(c), interceptor);
        }

        public Meta<T> interceptTraversal(Function<TreeTraversal, TreeTraversal> transform) {
            if (transform == Function.<TreeTraversal>identity()) {
                return this;
            }
            Function<TreeTraversal, TreeTraversal> newTransform = this.interceptor == Function.<TreeTraversal>identity() ? transform :
                Functions.compose(this.interceptor, transform);
            return new Meta<>(roots, traversal, expand, regard, this.filter, forceIgnore, forceDisregard, newTransform);
        }

        TreeTraversal.GuidedIt.Guide<T> createChildrenGuide(T parent) {
            return new TreeTraversal.GuidedIt.Guide<>() {
                final Predicate<? super T> expand = buildExpandConditionForChildren(parent);

                @Override
                public void guide(TreeTraversal.GuidedIt<T> guidedIt) {
                    doPerformChildrenGuidance(guidedIt, expand);
                }
            };
        }

        private void doPerformChildrenGuidance(TreeTraversal.GuidedIt<T> it, Predicate<? super T> expand) {
            if (it.curChild == null) {
                return;
            }
            if (forceIgnore.valueOr(it.curChild)) {
                return;
            }
            if (it.curParent == null || expand.test(it.curChild)) {
                it.queueNext(it.curChild);
            }
            else {
                it.result(it.curChild);
            }
        }

        private Predicate<? super T> buildExpandConditionForChildren(T parent) {
            // implements: or2(forceExpandAndSkip, not(childFilter));
            // and handles JBIterable.StatefulTransform and EdgeFilter conditions
            Cond copy = null;
            boolean invert = true;
            Cond c = regard;
            while (c != null) {
                Predicate impl = JBIterable.Stateful.copy(c.impl);
                if (impl != (invert ? Predicates.alwaysTrue() : Predicates.alwaysFalse())) {
                    copy = new Cond<Object>(invert ? Predicates.not(impl) : impl, copy);
                    if (impl instanceof EdgeFilter edgeFilter) {
                        edgeFilter.edgeSource = parent;
                    }
                }
                if (c.next == null) {
                    c = invert ? forceDisregard : null;
                    invert = false;
                }
                else {
                    c = c.next;
                }
            }
            return copy == null ? Predicates.alwaysFalse() : copy.OR;
        }

        private static final Meta<?> EMPTY = new Meta<Object>(
            JBIterable.empty(),
            TreeTraversal.PRE_ORDER_DFS,
            Cond.TRUE,
            Cond.TRUE,
            Cond.TRUE,
            Cond.FALSE,
            Cond.FALSE,
            Functions.<TreeTraversal>id()
        );

        public static <T> Meta<T> empty() {
            return (Meta<T>)EMPTY;
        }

    }

    private static class Cond<T> {
        static final Cond TRUE = new Cond<>(Predicates.alwaysTrue(), null);
        static final Cond FALSE = new Cond<>(Predicates.alwaysFalse(), null);

        final Predicate<? super T> impl;
        @Nullable
        final Cond<T> next;

        Cond(Predicate<? super T> impl, @Nullable Cond<T> next) {
            this.impl = impl;
            this.next = next;
        }

        Cond<T> append(Predicate<? super T> impl) {
            return new Cond<>(impl, this);
        }

        boolean valueAnd(T t) {
            for (Cond<T> c = this; c != null; c = c.next) {
                if (!c.impl.test(t)) {
                    return false;
                }
            }
            return true;
        }

        boolean valueOr(T t) {
            for (Cond<T> c = this; c != null; c = c.next) {
                if (c.impl.test(t)) {
                    return true;
                }
            }
            return false;
        }

        final Predicate<? super T> OR = this::valueOr;

        final Predicate<? super T> AND = this::valueAnd;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Cond{");
            for (Cond<T> c = this; c != null; c = c.next) {
                sb.append(JBIterator.toShortString(c.impl));
                if (c.next != null) {
                    sb.append(", ");
                }
            }
            return sb.append("}").toString();
        }
    }

}
