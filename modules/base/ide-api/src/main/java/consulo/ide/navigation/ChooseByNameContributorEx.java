// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.navigation;

import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface ChooseByNameContributorEx extends ChooseByNameContributor {

  void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter);

  /**
   * The names for a search, which also knows the pattern the user typed ({@link FindSymbolParameters#getLocalPatternName()}): a
   * contributor can offer names built from the pattern itself, like a URL path matched against route patterns. By default the
   * names of the search scope
   */
  default void processNames(Processor<String> processor, FindSymbolParameters parameters) {
    processNames(processor, parameters.getSearchScope(), parameters.getIdFilter());
  }

  void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters);

  /**
   * @deprecated Use {@link #processNames(Processor, GlobalSearchScope, IdFilter)} instead
   */
  @Deprecated
  @Override
  
  default String[] getNames(Project project, boolean includeNonProjectItems) {
    List<String> result = new ArrayList<>();
    processNames(result::add, FindSymbolParameters.searchScopeFor(project, includeNonProjectItems), null);
    return ArrayUtil.toStringArray(result);
  }

  /**
   * @deprecated Use {@link #processElementsWithName(String, Processor, FindSymbolParameters)} instead
   */
  @Deprecated
  @Override
  
  default NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    List<NavigationItem> result = new ArrayList<>();
    processElementsWithName(name, result::add, FindSymbolParameters.simple(project, includeNonProjectItems));
    return result.isEmpty() ? NavigationItem.EMPTY_ARRAY : result.toArray(NavigationItem.EMPTY_ARRAY);
  }
}
