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
public record LoadingOrder(String name, boolean first, boolean last, Set<String> before, Set<String> after) {
  public static final String FIRST_STR = "first";
  public static final String LAST_STR = "last";
  public static final String BEFORE_STR = "before ";
  public static final String AFTER_STR = "after ";

  public static final String ORDER_RULE_SEPARATOR = ",";

  public static final LoadingOrder ANY = new LoadingOrder("ANY", false, false, Set.of(), Set.of());
  public static final LoadingOrder FIRST = new LoadingOrder(FIRST_STR, true, false, Set.of(), Set.of());
  public static final LoadingOrder LAST = new LoadingOrder(LAST_STR, false, true, Set.of(), Set.of());

  public static LoadingOrder before(final String id) {
    return parse(BEFORE_STR + id);
  }

  public static LoadingOrder after(final String id) {
    return parse(AFTER_STR + id);
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
      if (order.first()) {
        first.add(o);
      }
      
      if (!order.before().isEmpty()) {
        hasBefore.add(o);
      }
    }

    if (cachedMap.isEmpty()) return;

    InboundSemiGraph<Orderable> graph = new InboundSemiGraph<>() {
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
        for (String id : order.after()) {
          Orderable o = map.get(id);
          if (o != null) {
            predecessors.add(o);
          }
        }

        String id = n.getOrderId();
        if (StringUtil.isNotEmpty(id)) {
          for (Orderable o : hasBefore) {
            LoadingOrder hisOrder = cachedMap.getOrDefault(o, ANY);
            if (hisOrder.before().contains(id)) {
              predecessors.add(o);
            }
          }
        }

        if (order.last()) {
          for (Orderable o : orderable) {
            LoadingOrder hisOrder = cachedMap.getOrDefault(o, ANY);
            if (!hisOrder.last()) {
              predecessors.add(o);
            }
          }
        }

        if (!order.first()) {
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
      return parse(orderAttr);
    }
  }

  private static LoadingOrder parse(String text) {
    boolean last = false;
    boolean first = false;

    Set<String> before = Set.of();
    Set<String> after = Set.of();

    for (final String string : StringUtil.split(text, ORDER_RULE_SEPARATOR)) {
      String trimmed = string.trim();
      if (trimmed.equalsIgnoreCase(FIRST_STR)) {
        first = true;
      }
      else if (trimmed.equalsIgnoreCase(LAST_STR)) {
        last = true;
      }
      else if (StringUtil.startsWithIgnoreCase(trimmed, BEFORE_STR)) {
        if (before.isEmpty()) {
          before = new LinkedHashSet<>();
        }

        before.add(trimmed.substring(BEFORE_STR.length()).trim());
      }
      else if (StringUtil.startsWithIgnoreCase(trimmed, AFTER_STR)) {
        if (after.isEmpty()) {
          after = new LinkedHashSet<>();
        }

        after.add(trimmed.substring(AFTER_STR.length()).trim());
      }
      else {
        throw new AssertionError("Invalid specification: " + trimmed + "; should be one of FIRST, LAST, BEFORE <id> or AFTER <id>");
      }
    }

    return new LoadingOrder(text, first, last, before, after);
  }

  public interface Orderable {
    @Nullable
    String getOrderId();

    LoadingOrder getOrder();
  }
}
