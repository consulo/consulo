// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.HiddenFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.ObjectUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FindSymbolParameters {
  private final String myCompletePattern;
  private final String myLocalPatternName;
  private final GlobalSearchScope mySearchScope;
  private final IdFilter myIdFilter;

  public FindSymbolParameters(@Nonnull String pattern, @Nonnull String name, @Nonnull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    myCompletePattern = pattern;
    myLocalPatternName = name;
    mySearchScope = scope;
    myIdFilter = idFilter;
  }

  public FindSymbolParameters withCompletePattern(@Nonnull String pattern) {
    return new FindSymbolParameters(pattern, myLocalPatternName, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withLocalPattern(@Nonnull String pattern) {
    return new FindSymbolParameters(myCompletePattern, pattern, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withScope(@Nonnull GlobalSearchScope scope) {
    return new FindSymbolParameters(myCompletePattern, myLocalPatternName, scope, myIdFilter);
  }

  @Nonnull
  public String getCompletePattern() {
    return myCompletePattern;
  }

  @Nonnull
  public String getLocalPatternName() {
    return myLocalPatternName;
  }

  @Nonnull
  public GlobalSearchScope getSearchScope() {
    return mySearchScope;
  }

  @Nullable
  public IdFilter getIdFilter() {
    return myIdFilter;
  }

  @Nonnull
  public Project getProject() {
    return ObjectUtils.notNull(mySearchScope.getProject());
  }

  public boolean isSearchInLibraries() {
    return mySearchScope.isSearchInLibraries();
  }

  public static FindSymbolParameters wrap(@Nonnull String pattern, @Nonnull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters(pattern, pattern, searchScopeFor(project, searchInLibraries), ((FileBasedIndexImpl)FileBasedIndex.getInstance()).projectIndexableFiles(project));
  }

  public static FindSymbolParameters wrap(@Nonnull String pattern, @Nonnull GlobalSearchScope scope) {
    return new FindSymbolParameters(pattern, pattern, scope, null);
  }

  public static FindSymbolParameters simple(@Nonnull Project project, boolean searchInLibraries) {
    return new FindSymbolParameters("", "", searchScopeFor(project, searchInLibraries), ((FileBasedIndexImpl)FileBasedIndex.getInstance()).projectIndexableFiles(project));
  }

  @Nonnull
  public static GlobalSearchScope searchScopeFor(@Nullable Project project, boolean searchInLibraries) {
    GlobalSearchScope baseScope = project == null ? new EverythingGlobalScope() : searchInLibraries ? ProjectScope.getAllScope(project) : ProjectScope.getProjectScope(project);

    return baseScope.intersectWith(new EverythingGlobalScope(project) {
      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        return !(file.getFileSystem() instanceof HiddenFileSystem);
      }
    });
  }
}
