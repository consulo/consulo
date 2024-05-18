// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

final class ServiceModelFilter {
  private final List<ServiceViewFilter> myFilters = new CopyOnWriteArrayList<>();

  void addFilter(@Nonnull ServiceViewFilter filter) {
    myFilters.add(filter);
  }

  void removeFilter(@Nonnull ServiceViewFilter filter) {
    ServiceViewFilter parent = filter.getParent();
    myFilters.remove(filter);
    for (ServiceViewFilter viewFilter : myFilters) {
      if (viewFilter.getParent() == filter) {
        viewFilter.setParent(parent);
      }
    }
  }

  @Nonnull
  List<? extends ServiceViewItem> filter(@Nonnull List<? extends ServiceViewItem> items, @Nonnull ServiceViewFilter targetFilter) {
    if (items.isEmpty()) return items;

    List<ServiceViewFilter> filters = excludeTargetAndParents(targetFilter);
    return ContainerUtil.filter(items, item -> !ContainerUtil.exists(filters, filter -> filter.test(item)));
  }

  private List<ServiceViewFilter> excludeTargetAndParents(@Nonnull ServiceViewFilter targetFilter) {
    List<ServiceViewFilter> filters = new ArrayList<>(myFilters);
    do {
      filters.remove(targetFilter);
      targetFilter = targetFilter.getParent();
    }
    while (targetFilter != null);
    return filters;
  }

  abstract static class ServiceViewFilter implements Predicate<ServiceViewItem> {
    private ServiceViewFilter myParent;

    protected ServiceViewFilter(@Nullable ServiceViewFilter parent) {
      myParent = parent;
    }

    @Nullable
    ServiceViewFilter getParent() {
      return myParent;
    }

    private void setParent(@Nullable ServiceViewFilter parent) {
      myParent = parent;
    }
  }
}
