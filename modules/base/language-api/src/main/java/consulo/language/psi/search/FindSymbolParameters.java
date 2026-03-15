// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopes;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.HiddenFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

public class FindSymbolParameters {
  private final String myCompletePattern;
  private final String myLocalPatternName;
  private final ProjectAwareSearchScope mySearchScope;
  private final IdFilter myIdFilter;

  public FindSymbolParameters(String pattern, String name, ProjectAwareSearchScope scope, @Nullable IdFilter idFilter) {
    myCompletePattern = pattern;
    myLocalPatternName = name;
    mySearchScope = scope;
    myIdFilter = idFilter;
  }

  public FindSymbolParameters withCompletePattern(String pattern) {
    return new FindSymbolParameters(pattern, myLocalPatternName, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withLocalPattern(String pattern) {
    return new FindSymbolParameters(myCompletePattern, pattern, mySearchScope, myIdFilter);
  }

  public FindSymbolParameters withScope(GlobalSearchScope scope) {
    return new FindSymbolParameters(myCompletePattern, myLocalPatternName, scope, myIdFilter);
  }

  
  public String getCompletePattern() {
    return myCompletePattern;
  }

  
  public String getLocalPatternName() {
    return myLocalPatternName;
  }

  
  public ProjectAwareSearchScope getSearchScope() {
    return mySearchScope;
  }

  @Nullable
  public IdFilter getIdFilter() {
    return myIdFilter;
  }

  
  public Project getProject() {
    return ObjectUtil.notNull(mySearchScope.getProject());
  }

  public boolean isSearchInLibraries() {
    return mySearchScope.isSearchInLibraries();
  }

  public static FindSymbolParameters wrap(String pattern, Project project, boolean searchInLibraries) {
    return new FindSymbolParameters(pattern, pattern, searchScopeFor(project, searchInLibraries), FileBasedIndex.getInstance().createProjectIndexableFiles(project));
  }

  public static FindSymbolParameters wrap(String pattern, GlobalSearchScope scope) {
    return new FindSymbolParameters(pattern, pattern, scope, null);
  }

  public static FindSymbolParameters simple(Project project, boolean searchInLibraries) {
    return new FindSymbolParameters("", "", searchScopeFor(project, searchInLibraries), FileBasedIndex.getInstance().createProjectIndexableFiles(project));
  }

  
  public static ProjectAwareSearchScope searchScopeFor(@Nullable Project project, boolean searchInLibraries) {
    ProjectAwareSearchScope baseScope = project == null ? new EverythingGlobalScope() : searchInLibraries ? ProjectScopes.getAllScope(project) : ProjectScopes.getProjectScope(project);

    return (ProjectAwareSearchScope)baseScope.intersectWith(new EverythingGlobalScope(project) {
      @Override
      public boolean contains(VirtualFile file) {
        return !(file.getFileSystem() instanceof HiddenFileSystem);
      }
    });
  }
}
