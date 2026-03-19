// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.ide.navigation.ChooseByNameContributor;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public abstract class FilteringGotoByModel<T> extends ContributorsBasedGotoByModel {
  /**
   * current file types
   */
  private Set<T> myFilterItems;

  protected FilteringGotoByModel(Project project, ChooseByNameContributor[] contributors) {
    super(project, contributors);
  }

  protected FilteringGotoByModel(Project project, List<? extends ChooseByNameContributor> contributors) {
    super(project, contributors);
  }

  /**
   * Set file types
   *
   * @param filterItems a file types to set
   */
  public synchronized void setFilterItems(Collection<? extends T> filterItems) {
    // get and set method are called from different threads
    myFilterItems = new HashSet<>(filterItems);
  }

  /**
   * @return get file types
   */
  protected synchronized @Nullable Collection<T> getFilterItems() {
    // get and set method are called from different threads
    return myFilterItems;
  }

  @Override
  protected boolean acceptItem(NavigationItem item) {
    T filterValue = filterValueFor(item);
    if (filterValue != null) {
      Collection<T> types = getFilterItems();
      return types == null || types.contains(filterValue);
    }
    return true;
  }

  protected abstract @Nullable T filterValueFor(NavigationItem item);
}
