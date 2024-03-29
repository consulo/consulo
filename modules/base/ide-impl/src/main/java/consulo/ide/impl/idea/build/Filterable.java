// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

public interface Filterable<T> {
  boolean isFilteringEnabled();

  @Nonnull
  Predicate<T> getFilter();

  void addFilter(@Nonnull Predicate<? super T> filter);

  void removeFilter(@Nonnull Predicate<? super T> filter);

  boolean contains(@Nonnull Predicate<? super T> filter);
}
