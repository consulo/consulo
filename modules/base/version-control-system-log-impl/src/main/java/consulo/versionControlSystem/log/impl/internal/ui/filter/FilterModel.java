/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

abstract class FilterModel<Filter extends VcsLogFilter> {
  @Nonnull
  private final String myName;
  @Nonnull
  protected final MainVcsLogUiProperties myUiProperties;
  @Nonnull
  private final Supplier<VcsLogDataPack> myDataPackProvider;
  @Nonnull
  private final Collection<Runnable> mySetFilterListeners = new ArrayList<>();

  @Nullable private Filter myFilter;

  FilterModel(@Nonnull String name, @Nonnull Supplier<VcsLogDataPack> provider, @Nonnull MainVcsLogUiProperties uiProperties) {
    myName = name;
    myUiProperties = uiProperties;
    myDataPackProvider = provider;
  }

  void setFilter(@Nullable Filter filter) {
    myFilter = filter;
    saveFilter(filter);
    for (Runnable listener : mySetFilterListeners) {
      listener.run();
    }
  }

  protected void saveFilter(@Nullable Filter filter) {
    myUiProperties.saveFilterValues(myName, filter == null ? null : getFilterValues(filter));
  }

  @Nullable
  Filter getFilter() {
    if (myFilter == null) {
      myFilter = getLastFilter();
    }
    return myFilter;
  }

  @Nullable
  protected abstract Filter createFilter(@Nonnull List<String> values);

  @Nonnull
  protected abstract List<String> getFilterValues(@Nonnull Filter filter);

  @Nullable
  protected Filter getLastFilter() {
    List<String> values = myUiProperties.getFilterValues(myName);
    if (values != null) {
      return createFilter(values);
    }
    return null;
  }

  @Nonnull
  VcsLogDataPack getDataPack() {
    return myDataPackProvider.get();
  }

  void addSetFilterListener(@Nonnull Runnable runnable) {
    mySetFilterListeners.add(runnable);
  }
}
