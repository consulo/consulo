/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.psi.scope;

import consulo.content.scope.BaseSearchScope;
import consulo.content.scope.SearchScope;
import consulo.language.content.FileIndexFacade;
import consulo.language.psi.PsiBundle;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.content.scope.ModuleScopeProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.UnloadedModuleDescription;
import consulo.module.content.scope.ModuleAwareSearchScope;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public abstract class GlobalSearchScope extends BaseSearchScope implements ModuleAwareSearchScope {
  private static final Logger LOG = Logger.getInstance(GlobalSearchScope.class);
  @Nullable
  private final Project myProject;

  protected GlobalSearchScope(@Nullable Project project) {
    myProject = project;
  }

  protected GlobalSearchScope() {
    this(null);
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  /**
   * @return <ul>
   * <li>a positive integer (e.g. +1), if file1 is located in the classpath before file2</li>
   * <li>a negative integer (e.e -1), if file1 is located in the classpath after file2</li>
   * <li>zero - otherwise or when the files are not comparable</li>
   * </ul>
   */
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    return 0;
  }

  // optimization methods:

  @Override
  public abstract boolean isSearchInModuleContent(@Nonnull Module aModule);

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
    return isSearchInModuleContent(aModule);
  }

  @Override
  public final boolean accept(VirtualFile file) {
    return contains(file);
  }

  @Override
  public abstract boolean isSearchInLibraries();

  @Override
  public boolean isForceSearchingInLibrarySources() {
    return false;
  }

  /**
   * Returns descriptions of unloaded modules content of whose might be included into this scope if they had been loaded. Actually search in
   * unloaded modules isn't performed, so this method is used to determine whether a warning about possible missing results should be shown.
   */
  @Override
  @Nonnull
  public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
    return Collections.emptySet();
  }

  @Override
  public boolean isSearchOutsideRootModel() {
    return false;
  }

  @Nonnull
  public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
    if (scope == this) return this;
    if (scope instanceof IntersectionScope) {
      return scope.intersectWith(this);
    }
    return new IntersectionScope(this, scope, null);
  }

  @Nonnull
  @Override
  public SearchScope intersectWith(@Nonnull SearchScope scope2) {
    if (scope2 instanceof LocalSearchScope) {
      LocalSearchScope localScope2 = (LocalSearchScope)scope2;
      return intersectWith(localScope2);
    }
    return intersectWith((GlobalSearchScope)scope2);
  }

  @Nonnull
  public SearchScope intersectWith(@Nonnull LocalSearchScope localScope2) {
    PsiElement[] elements2 = localScope2.getScope();
    List<PsiElement> result = new ArrayList<PsiElement>(elements2.length);
    for (PsiElement element2 : elements2) {
      if (PsiSearchScopeUtil.isInScope(this, element2)) {
        result.add(element2);
      }
    }
    return result.isEmpty() ? EMPTY_SCOPE : new LocalSearchScope(result.toArray(new PsiElement[result.size()]), null, localScope2.isIgnoreInjectedPsi());
  }

  @Override
  @Nonnull
  public GlobalSearchScope union(@Nonnull SearchScope scope) {
    if (scope instanceof GlobalSearchScope) return uniteWith((GlobalSearchScope)scope);
    return union((LocalSearchScope)scope);
  }

  @Nonnull
  public GlobalSearchScope union(@Nonnull final LocalSearchScope scope) {
    return new GlobalSearchScope(scope.getScope()[0].getProject()) {
      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        return GlobalSearchScope.this.contains(file) || scope.isInScope(file);
      }

      @Override
      public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
        return GlobalSearchScope.this.contains(file1) && GlobalSearchScope.this.contains(file2) ? GlobalSearchScope.this.compare(file1, file2) : 0;
      }

      @Override
      public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return GlobalSearchScope.this.isSearchInModuleContent(aModule);
      }

      @Override
      public boolean isSearchOutsideRootModel() {
        return GlobalSearchScope.this.isSearchOutsideRootModel();
      }

      @Override
      public boolean isSearchInLibraries() {
        return GlobalSearchScope.this.isSearchInLibraries();
      }

      @NonNls
      @Override
      public String toString() {
        return "UnionToLocal: (" + GlobalSearchScope.this.toString() + ", " + scope + ")";
      }
    };
  }

  @Nonnull
  public GlobalSearchScope uniteWith(@Nonnull GlobalSearchScope scope) {
    if (scope == this) return scope;

    return new UnionScope(this, scope);
  }

  @Nonnull
  @Contract(pure = true)
  public static GlobalSearchScope union(@Nonnull GlobalSearchScope[] scopes) {
    if (scopes.length == 0) {
      throw new IllegalArgumentException("Empty scope array");
    }
    if (scopes.length == 1) {
      return scopes[0];
    }
    return new UnionScope(scopes);
  }

  @Nonnull
  public static GlobalSearchScope allScope(@Nonnull Project project) {
    return (GlobalSearchScope)ProjectScopes.getAllScope(project);
  }

  @Nonnull
  @Contract(pure = true)
  public static GlobalSearchScope everythingScope(@Nonnull Project project) {
    return (GlobalSearchScope)ProjectScopes.getEverythingScope(project);
  }

  @Nonnull
  public static GlobalSearchScope projectScope(@Nonnull Project project) {
    return (GlobalSearchScope)ProjectScopes.getProjectScope(project);
  }

  @Nonnull
  public static GlobalSearchScope notScope(@Nonnull GlobalSearchScope scope) {
    return new NotScope(scope);
  }

  private static class NotScope extends DelegatingGlobalSearchScope {
    private NotScope(@Nonnull GlobalSearchScope scope) {
      super(scope);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return !myBaseScope.contains(file);
    }

    @Override
    public boolean isSearchInLibraries() {
      return true; // not (in library A) is perfectly fine to find classes in another library B.
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
      return true; // not (some files in module A) is perfectly fine to find classes in another part of module A.
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return true; // not (some files in module A) is perfectly fine to find classes in another part of module A.
    }

    @Override
    public boolean isSearchOutsideRootModel() {
      return true;
    }

    @Override
    public String toString() {
      return "NOT: " + myBaseScope;
    }
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @param module the module to get the scope.
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  @Nonnull
  public static GlobalSearchScope moduleScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleScope();
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @param module       the module to get the scope.
   * @param includeTests include tests or not
   * @return scope including sources and tests(if set includeTests), excluding libraries and dependencies.
   */
  @Nonnull
  public static GlobalSearchScope moduleScope(@Nonnull Module module, boolean includeTests) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleScope(includeTests);
  }

  @Nonnull
  public static GlobalSearchScope moduleContentScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleContentScope();
  }

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  public static GlobalSearchScope moduleWithLibrariesScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleWithLibrariesScope();
  }

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  public static GlobalSearchScope moduleWithDependenciesScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleWithDependenciesScope();
  }

  @Nonnull
  public static GlobalSearchScope moduleRuntimeScope(@Nonnull Module module, boolean includeTests) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleRuntimeScope(includeTests);
  }

  @Nonnull
  public static GlobalSearchScope moduleWithDependenciesAndLibrariesScope(@Nonnull Module module) {
    return moduleWithDependenciesAndLibrariesScope(module, true);
  }

  @Nonnull
  public static GlobalSearchScope moduleWithDependenciesAndLibrariesScope(@Nonnull Module module, boolean includeTests) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleWithDependenciesAndLibrariesScope(includeTests);
  }

  @Nonnull
  public static GlobalSearchScope moduleWithDependentsScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleWithDependentsScope();
  }

  @Nonnull
  public static GlobalSearchScope moduleTestsWithDependentsScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleTestsWithDependentsScope();
  }

  @Nonnull
  public static GlobalSearchScope moduleContentWithDependenciesScope(@Nonnull Module module) {
    return (GlobalSearchScope)ModuleScopeProvider.getInstance(module).getModuleContentWithDependenciesScope();
  }

  @Nonnull
  public static GlobalSearchScope fileScope(@Nonnull PsiFile psiFile) {
    return new FileScope(psiFile.getProject(), psiFile.getVirtualFile());
  }

  @Nonnull
  public static GlobalSearchScope fileScope(@Nonnull Project project, VirtualFile virtualFile) {
    return fileScope(project, virtualFile, null);
  }

  @Nonnull
  public static GlobalSearchScope fileScope(@Nonnull Project project, final VirtualFile virtualFile, @Nullable final String displayName) {
    return new FileScope(project, virtualFile) {
      @Nonnull
      @Override
      public String getDisplayName() {
        return displayName == null ? super.getDisplayName() : displayName;
      }
    };
  }

  @Nonnull
  public static GlobalSearchScope filesScope(@Nonnull Project project, @Nonnull Collection<VirtualFile> files) {
    return filesScope(project, files, null);
  }

  @Nonnull
  public static GlobalSearchScope filesScope(@Nonnull Project project, @Nonnull Collection<VirtualFile> files, @Nullable final String displayName) {
    if (files.isEmpty()) return EMPTY_SCOPE;
    return files.size() == 1 ? fileScope(project, files.iterator().next(), displayName) : new FilesScope(project, files) {
      @Nonnull
      @Override
      public String getDisplayName() {
        return displayName == null ? super.getDisplayName() : displayName;
      }
    };
  }

  /**
   * Optimization. By default FilesScope makes a decision about searching in libraries by checking that
   * at least one file is placed out of module roots. So if you're sure about files placement you can explicitly say FilesScope whether
   * it should include libraries or not in order to avoid checking each file.
   * Also, if you have a lot of files it might be faster to always search in libraries.
   */
  @Nonnull
  public static GlobalSearchScope filesWithoutLibrariesScope(@Nonnull Project project, @Nonnull Collection<VirtualFile> files) {
    if (files.isEmpty()) return EMPTY_SCOPE;
    return new FilesScope(project, files, false);
  }

  @Nonnull
  public static GlobalSearchScope filesWithLibrariesScope(@Nonnull Project project, @Nonnull Collection<VirtualFile> files) {
    if (files.isEmpty()) return EMPTY_SCOPE;
    return new FilesScope(project, files, true);
  }

  private static class IntersectionScope extends GlobalSearchScope {
    private final GlobalSearchScope myScope1;
    private final GlobalSearchScope myScope2;
    private final String myDisplayName;

    private IntersectionScope(@Nonnull GlobalSearchScope scope1, @Nonnull GlobalSearchScope scope2, String displayName) {
      super(scope1.getProject() == null ? scope2.getProject() : scope1.getProject());
      myScope1 = scope1;
      myScope2 = scope2;
      myDisplayName = displayName;
    }

    @Nonnull
    @Override
    public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
      if (myScope1.equals(scope) || myScope2.equals(scope)) {
        return this;
      }
      return new IntersectionScope(this, scope, null);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      if (myDisplayName == null) {
        return PsiBundle.message("psi.search.scope.intersection", myScope1.getDisplayName(), myScope2.getDisplayName());
      }
      return myDisplayName;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return myScope1.contains(file) && myScope2.contains(file);
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      int res1 = myScope1.compare(file1, file2);
      int res2 = myScope2.compare(file1, file2);

      if (res1 == 0) return res2;
      if (res2 == 0) return res1;

      res1 /= Math.abs(res1);
      res2 /= Math.abs(res2);
      if (res1 == res2) return res1;

      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return myScope1.isSearchInModuleContent(aModule) && myScope2.isSearchInModuleContent(aModule);
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
      return myScope1.isSearchInModuleContent(aModule, testSources) && myScope2.isSearchInModuleContent(aModule, testSources);
    }

    @Override
    public boolean isSearchInLibraries() {
      return myScope1.isSearchInLibraries() && myScope2.isSearchInLibraries();
    }

    @Override
    public boolean isSearchOutsideRootModel() {
      return myScope1.isSearchOutsideRootModel() && myScope2.isSearchOutsideRootModel();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof IntersectionScope)) return false;

      IntersectionScope that = (IntersectionScope)o;

      return myScope1.equals(that.myScope1) && myScope2.equals(that.myScope2);
    }

    @Override
    public int hashCode() {
      return 31 * myScope1.hashCode() + myScope2.hashCode();
    }

    @NonNls
    @Override
    public String toString() {
      return "Intersection: (" + myScope1 + ", " + myScope2 + ")";
    }
  }

  private static class UnionScope extends GlobalSearchScope {
    private final GlobalSearchScope[] myScopes;
    private final int myNestingLevel;

    private UnionScope(@Nonnull GlobalSearchScope scope1, @Nonnull GlobalSearchScope scope2) {
      this(new GlobalSearchScope[]{scope1, scope2});
    }

    private UnionScope(@Nonnull GlobalSearchScope[] scopes) {
      super(ContainerUtil.getFirstItem(ContainerUtil.mapNotNull(scopes, scope -> scope.getProject()), null));
      assert scopes.length > 1 : Arrays.asList(scopes);
      myScopes = scopes;
      int[] nested = {0};
      for (GlobalSearchScope scope : scopes) {
        nested[0] = Math.max(nested[0], scope instanceof UnionScope ? ((UnionScope)scope).myNestingLevel : 0);
      }
      myNestingLevel = 1 + nested[0];
      if (myNestingLevel > 1000) {
        throw new IllegalStateException("Too many scopes combined: " + myNestingLevel + StringUtil.first(toString(), 500, true));
      }
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return PsiBundle.message("psi.search.scope.union", myScopes[0].getDisplayName(), myScopes[1].getDisplayName());
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return ContainerUtil.find(myScopes, scope -> scope.contains(file)) != null;
    }

    @Override
    public boolean isSearchOutsideRootModel() {
      return ContainerUtil.find(myScopes, scope -> scope.isSearchOutsideRootModel()) != null;
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      int[] result = {0};
      ContainerUtil.process(myScopes, scope -> {
        int res1 = scope.contains(file1) && scope.contains(file2) ? scope.compare(file1, file2) : 0;
        if (result[0] == 0) {
          result[0] = res1;
          return true;
        }
        if ((result[0] > 0) != (res1 > 0)) {
          result[0] = 0;
          return false;
        }
        return true;
      });
      return result[0];
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module module) {
      return ContainerUtil.find(myScopes, scope -> scope.isSearchInModuleContent(module)) != null;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module module, boolean testSources) {
      return ContainerUtil.find(myScopes, scope -> scope.isSearchInModuleContent(module, testSources)) != null;
    }

    @Override
    public boolean isSearchInLibraries() {
      return ContainerUtil.find(myScopes, scope -> scope.isSearchInLibraries()) != null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof UnionScope)) return false;

      UnionScope that = (UnionScope)o;

      return new HashSet<GlobalSearchScope>(Arrays.asList(myScopes)).equals(new HashSet<GlobalSearchScope>(Arrays.asList(that.myScopes)));
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(myScopes);
    }

    @NonNls
    @Override
    public String toString() {
      return "Union: (" + StringUtil.join(Arrays.asList(myScopes), ",") + ")";
    }

    @Nonnull
    @Override
    public GlobalSearchScope uniteWith(@Nonnull GlobalSearchScope scope) {
      if (scope instanceof UnionScope) {
        GlobalSearchScope[] newScopes = ArrayUtil.mergeArrays(myScopes, ((UnionScope)scope).myScopes);
        return new UnionScope(newScopes);
      }
      return super.uniteWith(scope);
    }
  }

  @Nonnull
  public static GlobalSearchScope getScopeRestrictedByFileTypes(@Nonnull GlobalSearchScope scope, @Nonnull FileType... fileTypes) {
    if (scope == EMPTY_SCOPE) {
      return EMPTY_SCOPE;
    }
    LOG.assertTrue(fileTypes.length > 0);
    return new FileTypeRestrictionScope(scope, fileTypes);
  }

  private static class FileTypeRestrictionScope extends DelegatingGlobalSearchScope {
    private final FileType[] myFileTypes;

    private FileTypeRestrictionScope(@Nonnull GlobalSearchScope scope, @Nonnull FileType[] fileTypes) {
      super(scope);
      myFileTypes = fileTypes;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      if (!super.contains(file)) return false;

      FileType fileType = file.getFileType();
      for (FileType otherFileType : myFileTypes) {
        if (fileType.equals(otherFileType)) return true;
      }

      return false;
    }

    @Nonnull
    @Override
    public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
      if (scope instanceof FileTypeRestrictionScope) {
        FileTypeRestrictionScope restrict = (FileTypeRestrictionScope)scope;
        if (restrict.myBaseScope == myBaseScope) {
          List<FileType> intersection = new ArrayList<FileType>(Arrays.asList(restrict.myFileTypes));
          intersection.retainAll(Arrays.asList(myFileTypes));
          return new FileTypeRestrictionScope(myBaseScope, intersection.toArray(new FileType[intersection.size()]));
        }
      }
      return super.intersectWith(scope);
    }

    @Nonnull
    @Override
    public GlobalSearchScope uniteWith(@Nonnull GlobalSearchScope scope) {
      if (scope instanceof FileTypeRestrictionScope) {
        FileTypeRestrictionScope restrict = (FileTypeRestrictionScope)scope;
        if (restrict.myBaseScope == myBaseScope) {
          return new FileTypeRestrictionScope(myBaseScope, ArrayUtil.mergeArrays(myFileTypes, restrict.myFileTypes));
        }
      }
      return super.uniteWith(scope);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof FileTypeRestrictionScope)) return false;
      if (!super.equals(o)) return false;

      FileTypeRestrictionScope that = (FileTypeRestrictionScope)o;

      return Arrays.equals(myFileTypes, that.myFileTypes);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + Arrays.hashCode(myFileTypes);
      return result;
    }

    @Override
    public String toString() {
      return "(" + myBaseScope + " restricted by file types: " + Arrays.asList(myFileTypes) + ")";
    }
  }

  private static class EmptyScope extends GlobalSearchScope {
    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return false;
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return false;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }

    @Override
    @Nonnull
    public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
      return this;
    }

    @Override
    @Nonnull
    public GlobalSearchScope uniteWith(@Nonnull GlobalSearchScope scope) {
      return scope;
    }

    @Override
    public String toString() {
      return "EMPTY";
    }
  }

  public static final GlobalSearchScope EMPTY_SCOPE = new EmptyScope();

  private static class FileScope extends GlobalSearchScope implements Iterable<VirtualFile> {
    private final VirtualFile myVirtualFile; // files can be out of project roots
    private final Module myModule;

    private FileScope(@Nonnull Project project, VirtualFile virtualFile) {
      super(project);
      myVirtualFile = virtualFile;
      myModule = virtualFile == null || project.isDefault() ? null : FileIndexFacade.getInstance(project).getModuleForFile(virtualFile);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return Comparing.equal(myVirtualFile, file);
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return aModule == myModule;
    }

    @Override
    public boolean isSearchInLibraries() {
      return myModule == null;
    }

    @Override
    public String toString() {
      return "File :" + myVirtualFile;
    }

    @Override
    public Iterator<VirtualFile> iterator() {
      return Collections.singletonList(myVirtualFile).iterator();
    }
  }

  public static class FilesScope extends GlobalSearchScope implements Iterable<VirtualFile> {
    private final Collection<VirtualFile> myFiles;
    private volatile Boolean myHasFilesOutOfProjectRoots;

    /**
     * @deprecated use {@link GlobalSearchScope#filesScope(Project, Collection)}
     */
    public FilesScope(@Nullable Project project, @Nonnull Collection<VirtualFile> files) {
      this(project, files, null);
    }

    // Optimization
    private FilesScope(@Nullable Project project, @Nonnull Collection<VirtualFile> files, @Nullable Boolean hasFilesOutOfProjectRoots) {
      super(project);
      myFiles = files;
      myHasFilesOutOfProjectRoots = hasFilesOutOfProjectRoots;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return myFiles.contains(file);
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return hasFilesOutOfProjectRoots();
    }

    @Override
    public boolean equals(Object o) {
      return this == o || o instanceof FilesScope && myFiles.equals(((FilesScope)o).myFiles);
    }

    @Override
    public int hashCode() {
      return myFiles.hashCode();
    }

    private boolean hasFilesOutOfProjectRoots() {
      Boolean result = myHasFilesOutOfProjectRoots;
      if (result == null) {
        Project project = getProject();
        myHasFilesOutOfProjectRoots = result = project != null && !project.isDefault() && myFiles.stream().anyMatch(file -> FileIndexFacade.getInstance(project).getModuleForFile(file) == null);
      }
      return result;
    }

    @Override
    public String toString() {
      List<VirtualFile> files = myFiles.size() <= 20 ? new ArrayList<>(myFiles) : new ArrayList<>(myFiles).subList(0, 20);
      return "Files: (" + files + "); search in libraries: " + (myHasFilesOutOfProjectRoots != null ? myHasFilesOutOfProjectRoots : "unknown");
    }

    @Nonnull
    @Override
    public Iterator<VirtualFile> iterator() {
      return myFiles.iterator();
    }
  }
}
