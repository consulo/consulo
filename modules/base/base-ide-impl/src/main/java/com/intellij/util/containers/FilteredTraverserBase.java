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
package com.intellij.util.containers;

import consulo.application.util.function.Condition;
import consulo.application.util.function.Conditions;
import com.intellij.util.Function;
import com.intellij.util.Functions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static consulo.application.util.function.Conditions.not;

public abstract class FilteredTraverserBase<T, Self extends FilteredTraverserBase<T, Self>> implements Iterable<T> {

  private final Meta<T> myMeta;
  private final Function<T, ? extends Iterable<? extends T>> myTree;

  protected FilteredTraverserBase(@Nullable Meta<T> meta, @Nonnull Function<T, ? extends Iterable<? extends T>> tree) {
    this.myTree = tree;
    this.myMeta = meta == null ? Meta.<T>empty() : meta;
  }

  @Nonnull
  public Function<T, ? extends Iterable<? extends T>> getTree() {
    return myTree;
  }

  @Nonnull
  public final T getRoot() {
    return myMeta.roots.iterator().next();
  }

  @Nonnull
  public final Iterable<? extends T> getRoots() {
    return myMeta.roots;
  }

  @Override
  public final Iterator<T> iterator() {
    return traverse().iterator();
  }

  @Nonnull
  protected abstract Self newInstance(Meta<T> meta);

  @Nonnull
  public final JBIterable<T> traverse(@Nonnull TreeTraversal traversal) {
    Function<T, Iterable<? extends T>> adjusted = new Function<T, Iterable<? extends T>>() {
      @Override
      public Iterable<? extends T> fun(T t) {
        return children(t);
      }
    };
    return myMeta.interceptor.fun(traversal).traversal(getRoots(), adjusted).filter(myMeta.filter.AND);
  }

  @Nonnull
  public final JBIterable<T> traverse() {
    return traverse(myMeta.traversal);
  }

  @Nonnull
  public final JBIterable<T> preOrderDfsTraversal() {
    return traverse(TreeTraversal.PRE_ORDER_DFS);
  }

  @Nonnull
  public final JBIterable<T> postOrderDfsTraversal() {
    return traverse(TreeTraversal.POST_ORDER_DFS);
  }

  @Nonnull
  public final JBIterable<T> bfsTraversal() {
    return traverse(TreeTraversal.PLAIN_BFS);
  }

  @Nonnull
  public final JBIterable<T> tracingBfsTraversal() {
    return traverse(TreeTraversal.TRACING_BFS);
  }

  /**
   * Clears expand, regard and filter conditions, traversal while keeping roots and "forced" properties.
   * @see FilteredTraverserBase#forceIgnore(Condition)
   * @see FilteredTraverserBase#forceDisregard(Condition)
   */
  @Nonnull
  public final Self reset() {
    return newInstance(myMeta.reset());
  }

  @Nonnull
  public final Self withRoot(@Nullable T root) {
    return newInstance(myMeta.withRoots(ContainerUtil.createMaybeSingletonList(root)));
  }

  @Nonnull
  public final Self withRoots(@Nonnull Iterable<? extends T> roots) {
    return newInstance(myMeta.withRoots(roots));
  }

  @Nonnull
  public final Self withTraversal(TreeTraversal type) {
    return newInstance(myMeta.withTraversal(type));
  }

  /**
   * Restricts the nodes that can have children by the specified condition.
   * Subsequent calls will AND all the conditions.
   */
  @Nonnull
  public final Self expand(@Nonnull Condition<? super T> c) {
    return newInstance(myMeta.expand(c));
  }

  /**
   * Restricts the nodes that can be children by the specified condition while <b>keeping the edges</b>.
   * Subsequent calls will AND all the conditions.
   */
  @Nonnull
  public final Self regard(@Nonnull Condition<? super T> c) {
    return newInstance(myMeta.regard(c));
  }

  @Nonnull
  public final Self expandAndFilter(Condition<? super T> c) {
    return newInstance(myMeta.expand(c).filter(c));
  }

  @Nonnull
  public final Self expandAndSkip(Condition<? super T> c) {
    return newInstance(myMeta.expand(c).filter(not(c)));
  }

  @Nonnull
  public final Self filter(@Nonnull Condition<? super T> c) {
    return newInstance(myMeta.filter(c));
  }

  @Nonnull
  public final <C> JBIterable<C> filter(@Nonnull Class<C> type) {
    return traverse().filter(type);
  }

  /**
   * Configures the traverser to skip already visited nodes.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see FilteredTraverserBase#unique(Function)
   * @see TreeTraversal#unique()
   */
  @Nonnull
  public final Self unique() {
    return unique(Functions.identity());
  }

  /**
   * Configures the traverser to skip already visited nodes.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see TreeTraversal#unique(Function)
   */
  @Nonnull
  public final Self unique(@Nonnull final Function<? super T, Object> identity) {
    return interceptTraversal(new Function<TreeTraversal, TreeTraversal>() {
      @Override
      public TreeTraversal fun(TreeTraversal traversal) {
        return traversal.unique(identity);
      }
    });
  }

  /**
   * Configures the traverser to expand and return the nodes within the range only.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see TreeTraversal#onRange(Condition)
   */
  @Nonnull
  public Self onRange(@Nonnull final Condition<? super T> rangeCondition) {
    return interceptTraversal(new Function<TreeTraversal, TreeTraversal>() {
      @Override
      public TreeTraversal fun(TreeTraversal traversal) {
        return traversal.onRange(rangeCondition);
      }
    });
  }

  /**
   * Excludes the nodes by the specified condition from any traversal completely.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see FilteredTraverserBase#expand(Condition)
   * @see FilteredTraverserBase#filter(Condition)
   * @see FilteredTraverserBase#reset()
   */
  @Nonnull
  public final Self forceIgnore(@Nonnull Condition<? super T> c) {
    return newInstance(myMeta.forceIgnore(c));
  }

  /**
   * Excludes the nodes by the specified condition while keeping their edges.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see FilteredTraverserBase#regard(Condition)
   * @see FilteredTraverserBase#reset()
   */
  @Nonnull
  public final Self forceDisregard(@Nonnull Condition<? super T> c) {
    return newInstance(myMeta.forceDisregard(c));
  }

  /**
   * Intercepts and alters traversal just before the walking.
   * <p/>
   * This property is not reset by {@code reset()} call.
   * @see FilteredTraverserBase#unique()
   * @see FilteredTraverserBase#onRange(Condition)
   */
  @Nonnull
  public final Self interceptTraversal(@Nonnull Function<TreeTraversal, TreeTraversal> transform) {
    return newInstance(myMeta.interceptTraversal(transform));
  }

  /**
   * Returns the children of the specified node as seen by this traverser.
   * @see TreeTraversal.TracingIt to obtain parents of a node during a traversal
   */
  @Nonnull
  public final JBIterable<T> children(@Nullable T node) {
    if (node == null || isAlwaysLeaf(node)) {
      return JBIterable.empty();
    }
    else if (myMeta.regard.next == null && myMeta.forceDisregard.next == null) {
      return JBIterable.<T>from(myTree.fun(node)).filter(not(myMeta.forceIgnore.OR));
    }
    else {
      // traverse subtree to select accepted children
      return TreeTraversal.GUIDED_TRAVERSAL(myMeta.createChildrenGuide(node)).traversal(node, myTree);
    }
  }

  protected boolean isAlwaysLeaf(@Nonnull T node) {
    return !myMeta.expand.valueAnd(node);
  }

  @Nonnull
  public final List<T> toList() {
    return traverse().toList();
  }

  @Nonnull
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

    protected T edgeSource;

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

    public Meta(@Nonnull Iterable<? extends T> roots,
                @Nonnull TreeTraversal traversal,
                @Nonnull Cond<T> expand,
                @Nonnull Cond<T> regard,
                @Nonnull Cond<T> filter,
                @Nonnull Cond<T> forceIgnore,
                @Nonnull Cond<T> forceDisregard,
                @Nonnull Function<TreeTraversal, TreeTraversal> interceptor) {
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
      return new Meta<T>(roots, e.traversal, e.expand, e.regard, e.filter, forceIgnore, forceDisregard, e.interceptor);
    }

    public Meta<T> withRoots(@Nonnull Iterable<? extends T> roots) {
      return new Meta<T>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard, interceptor);
    }

    public Meta<T> withTraversal(TreeTraversal traversal) {
      return new Meta<T>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard, interceptor);
    }

    public Meta<T> expand(@Nonnull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand.append(c), regard, this.filter, forceIgnore, forceDisregard, interceptor);
    }

    public Meta<T> regard(@Nonnull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard.append(c), this.filter, forceIgnore, forceDisregard, interceptor);
    }

    public Meta<T> filter(@Nonnull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter.append(c), forceIgnore, forceDisregard, interceptor);
    }

    public Meta<T> forceIgnore(Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter, forceIgnore.append(c), forceDisregard, interceptor);
    }

    public Meta<T> forceDisregard(Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter, forceIgnore, forceDisregard.append(c), interceptor);
    }

    public Meta<T> interceptTraversal(Function<TreeTraversal, TreeTraversal> transform) {
      if (transform == Function.ID) return this;
      Function<TreeTraversal, TreeTraversal> newTransform = this.interceptor == Function.ID ? transform :
                                                            Functions.compose(this.interceptor, transform);
      return new Meta<T>(roots, traversal, expand, regard, this.filter, forceIgnore, forceDisregard, newTransform);
    }

    TreeTraversal.GuidedIt.Guide<T> createChildrenGuide(final T parent) {
      return new TreeTraversal.GuidedIt.Guide<T>() {
        final Condition<? super T> expand = buildExpandConditionForChildren(parent);
        @Override
        public void guide(@Nonnull TreeTraversal.GuidedIt<T> guidedIt) {
          doPerformChildrenGuidance(guidedIt, expand);
        }
      };
    }

    private void doPerformChildrenGuidance(TreeTraversal.GuidedIt<T> it, Condition<? super T> expand) {
      if (it.curChild == null) return;
      if (forceIgnore.valueOr(it.curChild)) return;
      if (it.curParent == null || expand.value(it.curChild)) {
        it.queueNext(it.curChild);
      }
      else {
        it.result(it.curChild);
      }
    }

    private Condition<? super T> buildExpandConditionForChildren(T parent) {
      // implements: or2(forceExpandAndSkip, not(childFilter));
      // and handles JBIterable.StatefulTransform and EdgeFilter conditions
      Cond copy = null;
      boolean invert = true;
      Cond c = regard;
      while (c != null) {
        Condition impl = JBIterable.Stateful.copy(c.impl);
        if (impl != (invert ? Condition.TRUE : Condition.FALSE)) {
          copy = new Cond<Object>(invert ? not(impl) : impl, copy);
          if (impl instanceof EdgeFilter) {
            ((EdgeFilter)impl).edgeSource = parent;
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
      return copy == null ? Condition.FALSE : copy.OR;
    }

    private static final Meta<?> EMPTY = new Meta<Object>(
            JBIterable.empty(), TreeTraversal.PRE_ORDER_DFS,
            Cond.TRUE, Cond.TRUE, Cond.TRUE,
            Cond.FALSE, Cond.FALSE, Functions.<TreeTraversal>id());

    public static <T> Meta<T> empty() {
      return (Meta<T>)EMPTY;
    }

  }

  private static class Cond<T> {
    final static Cond TRUE = new Cond<Object>(Conditions.TRUE, null);
    final static Cond FALSE = new Cond<Object>(Conditions.FALSE, null);

    final Condition<? super T> impl;
    final Cond<T> next;

    Cond(Condition<? super T> impl, Cond<T> next) {
      this.impl = impl;
      this.next = next;
    }

    Cond<T> append(Condition<? super T> impl) {
      return new Cond<T>(impl, this);
    }

    boolean valueAnd(T t) {
      for (Cond<T> c = this; c != null; c = c.next) {
        if (!c.impl.value(t)) return false;
      }
      return true;
    }

    boolean valueOr(T t) {
      for (Cond<T> c = this; c != null; c = c.next) {
        if (c.impl.value(t)) return true;
      }
      return false;
    }

    final Condition<? super T> OR = new Condition<T>() {
      @Override
      public boolean value(T t) {
        return valueOr(t);
      }
    };

    final Condition<? super T> AND = new Condition<T>() {
      @Override
      public boolean value(T t) {
        return valueAnd(t);
      }
    };

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("Cond{");
      for (Cond<T> c = this; c != null; c = c.next) {
        sb.append(JBIterator.toShortString(c.impl));
        if (c.next != null) sb.append(", ");
      }
      return sb.append("}").toString();
    }
  }

}
