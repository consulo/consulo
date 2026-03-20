// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import java.util.function.Predicate;

public interface Filterable<T> {
  boolean isFilteringEnabled();

  
  Predicate<T> getFilter();

  void addFilter(Predicate<? super T> filter);

  void removeFilter(Predicate<? super T> filter);

  boolean contains(Predicate<? super T> filter);
}
