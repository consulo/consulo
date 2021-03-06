// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public interface SearchScopeProvider {
  ExtensionPointName<SearchScopeProvider> EP_NAME = ExtensionPointName.create("com.intellij.searchScopesProvider");

  @Nullable
  default String getDisplayName() {
    return null;
  }

  @Nonnull
  default List<SearchScope> getSearchScopes(@Nonnull Project project) {
    return Collections.emptyList();
  }

  /**
   * General project scopes are added after 'Project', 'Everything' but before 'Production', 'Tests', etc.
   *
   * @param project
   * @see PredefinedSearchScopeProvider
   */
  default List<SearchScope> getGeneralSearchScopes(@Nonnull Project project) {
    return Collections.emptyList();
  }
}
