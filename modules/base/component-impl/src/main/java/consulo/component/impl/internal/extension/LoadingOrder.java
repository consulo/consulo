// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.impl.internal.extension;

import consulo.component.extension.ExtensionPoint;
import consulo.component.util.graph.CachingSemiGraph;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.GraphGenerator;
import consulo.component.util.graph.InboundSemiGraph;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * All extensions can have an "order" attribute in their XML element that will affect the place where this extension will appear in the
 * {@link ExtensionPoint#getExtensions()}. Possible values are "first", "last", "before ID" and "after ID" where ID
 * is another same-type extension ID. Values can be combined in a comma-separated way. E.g. if you wish to plug before some extension XXX
 * that has "first" as its order, you must be "first, before XXX". The same with "last".<p>
 * <p>
 * Extension ID can be specified in the "id" attribute of corresponding XML element. When you specify order, it's usually a good practice
 * to specify also id, to allow other plugin-writers to plug relatively to your extension.<p>
 * <p>
 * If some anchor id can't be resolved, the constraint is ignored.
 *
 * @author Alexander Kireyev
 */
public class LoadingOrder {
  public static final String FIRST_STR = "first";
  public static final String LAST_STR = "last";
  public static final String BEFORE_STR = "before ";
  public static final String BEFORE_STR_OLD = "before:";
  public static final String AFTER_STR = "after ";
  public static final String AFTER_STR_OLD = "after:";

  public static final String ORDER_RULE_SEPARATOR = ",";

  public static final LoadingOrder ANY = new LoadingOrder();
  public static final LoadingOrder FIRST = new LoadingOrder(FIRST_STR);
  public static final LoadingOrder LAST = new LoadingOrder(LAST_STR);

  private final String myName; // for debug only
  private final boolean myFirst;
  private final boolean myLast;
  private final Set<String> myBefore = new LinkedHashSet<>(2);
  private final Set<String> myAfter = new LinkedHashSet<>(2);

  private LoadingOrder() {
    myName = "ANY";
    myFirst = false;
    myLast = false;
  }

  private LoadingOrder(@Nonnull String text) {
    myName = text;
    boolean last = false;
    boolean first = false;
    for (final String string : StringUtil.split(text, ORDER_RULE_SEPARATOR)) {
      String trimmed = string.trim();
      if (trimmed.equalsIgnoreCase(FIRST_STR)) first = true;
      else if (trimmed.equalsIgnoreCase(LAST_STR)) last = true;
      else if (StringUtil.startsWithIgnoreCase(trimmed, BEFORE_STR)) myBefore.add(trimmed.substring(BEFORE_STR.length()).trim());
      else if (StringUtil.startsWithIgnoreCase(trimmed, BEFORE_STR_OLD)) myBefore.add(trimmed.substring(BEFORE_STR_OLD.length()).trim());
      else if (StringUtil.startsWithIgnoreCase(trimmed, AFTER_STR)) myAfter.add(trimmed.substring(AFTER_STR.length()).trim());
      else if (StringUtil.startsWithIgnoreCase(trimmed, AFTER_STR_OLD)) myAfter.add(trimmed.substring(AFTER_STR_OLD.length()).trim());
      else throw new AssertionError("Invalid specification: " + trimmed + "; should be one of FIRST, LAST, BEFORE <id> or AFTER <id>");
    }
    myFirst = first;
    myLast = last;
  }

  public String toString() {
    return myName;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof LoadingOrder)) return false;

    final LoadingOrder that = (LoadingOrder)o;

    if (myFirst != that.myFirst) return false;
    if (myLast != that.myLast) return false;
    if (!myAfter.equals(that.myAfter)) return false;
    if (!myBefore.equals(that.myBefore)) return false;

    return true;
  }

  public int hashCode() {
    int result = myFirst ? 1 : 0;
    result = 31 * result + (myLast ? 1 : 0);
    result = 31 * result + myBefore.hashCode();
    result = 31 * result + myAfter.hashCode();
    return result;
  }

  public static LoadingOrder before(@NonNls final String id) {
    return new LoadingOrder(BEFORE_STR + id);
  }

  public static LoadingOrder after(@NonNls final String id) {
    return new LoadingOrder(AFTER_STR + id);
  }

  public static void sort(@Nonnull Orderable[] orderable) {
    if (orderable.length > 1) {
      sort(Arrays.asList(orderable));
    }
  }

  public static void sort(@Nonnull final List<? extends Orderable> orderable) {
    if (orderable.size() < 2) return;

    // our graph is pretty sparse so do benefit from the fact
    final Map<String, Orderable> map = new LinkedHashMap<>();
    final Map<Orderable, LoadingOrder> cachedMap = new LinkedHashMap<>();
    final Set<Orderable> first = new LinkedHashSet<>(1);
    final Set<Orderable> hasBefore = new LinkedHashSet<>(orderable.size());
    for (Orderable o : orderable) {
      String id = o.getOrderId();
      if (StringUtil.isNotEmpty(id)) map.put(id, o);
      LoadingOrder order = o.getOrder();
      if (order == ANY) continue;

      cachedMap.put(o, order);
      if (order.myFirst) first.add(o);
      if (!order.myBefore.isEmpty()) hasBefore.add(o);
    }

    if (cachedMap.isEmpty()) return;

    InboundSemiGraph<Orderable> graph = new InboundSemiGraph<Orderable>() {
      @Nonnull
      @Override
      public Collection<Orderable> getNodes() {
        List<Orderable> list = ContainerUtil.newArrayList(orderable);
        Collections.reverse(list);
        return list;
      }

      @Nonnull
      @Override
      public Iterator<Orderable> getIn(Orderable n) {
        LoadingOrder order = cachedMap.getOrDefault(n, ANY);

        Set<Orderable> predecessors = new LinkedHashSet<>();
        for (String id : order.myAfter) {
          Orderable o = map.get(id);
          if (o != null) {
            predecessors.add(o);
          }
        }

        String id = n.getOrderId();
        if (StringUtil.isNotEmpty(id)) {
          for (Orderable o : hasBefore) {
            LoadingOrder hisOrder = cachedMap.getOrDefault(o, ANY);
            if (hisOrder.myBefore.contains(id)) {
              predecessors.add(o);
            }
          }
        }

        if (order.myLast) {
          for (Orderable o : orderable) {
            LoadingOrder hisOrder = cachedMap.getOrDefault(o, ANY);
            if (!hisOrder.myLast) {
              predecessors.add(o);
            }
          }
        }

        if (!order.myFirst) {
          predecessors.addAll(first);
        }

        return predecessors.iterator();
      }
    };

    DFSTBuilder<Orderable> builder = new DFSTBuilder<>(GraphGenerator.generate(CachingSemiGraph.cache(graph)));

    if (!builder.isAcyclic()) {
      Couple<Orderable> p = builder.getCircularDependency();
      throw new SortingException("Could not satisfy sorting requirements", p.first, p.second);
    }

    orderable.sort(builder.comparator());
  }

  @Nonnull
  public static LoadingOrder readOrder(@Nullable String orderAttr) {
    if (StringUtil.isEmptyOrSpaces(orderAttr)) {
      return ANY;
    }
    else if (orderAttr.equals(FIRST_STR)) {
      return FIRST;
    }
    else if (orderAttr.equals(LAST_STR)) {
      return LAST;
    }
    else {
      return new LoadingOrder(orderAttr);
    }
  }

  public interface Orderable {
    @Nullable
    String getOrderId();

    LoadingOrder getOrder();
  }
}
