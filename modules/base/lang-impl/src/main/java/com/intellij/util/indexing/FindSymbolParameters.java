/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.util.indexing;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

public class FindSymbolParameters {
  public static FindSymbolParameters simple(@Nonnull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters("", "", searchScopeFor(project, searchInLibraries), ((FileBasedIndexImpl)FileBasedIndex.getInstance()).projectIndexableFiles(project));
  }

  private final String myCompletePattern;
  private final String myLocalPatternName;
  private final GlobalSearchScope mySearchScope;
  private volatile IdFilter myIdFilter;

  public FindSymbolParameters(@Nonnull String pattern, @Nonnull String name, @Nonnull GlobalSearchScope scope, @javax.annotation.Nullable IdFilter idFilter) {
    myCompletePattern = pattern;
    myLocalPatternName = name;
    mySearchScope = scope;
    myIdFilter = idFilter;
  }

  public String getCompletePattern() {
    return myCompletePattern;
  }

  public String getLocalPatternName() {
    return myLocalPatternName;
  }

  public @Nonnull
  GlobalSearchScope getSearchScope() {
    return mySearchScope;
  }

  public @Nullable IdFilter getIdFilter() {
    return myIdFilter;
  }

  public void setIdFilter(IdFilter idFilter) {
    myIdFilter = idFilter;
  }

  public static FindSymbolParameters wrap(@Nonnull String pattern, @Nonnull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters(
      pattern,
      pattern,
      searchScopeFor(project, searchInLibraries),
      null
    );
  }

  public static GlobalSearchScope searchScopeFor(Project project, boolean searchInLibraries) {
    return searchInLibraries? ProjectScope.getAllScope(project) : ProjectScope.getProjectScope(project);
  }

  public Project getProject() {
    return mySearchScope.getProject();
  }

  public boolean isSearchInLibraries() {
    return mySearchScope.isSearchInLibraries();
  }
}
