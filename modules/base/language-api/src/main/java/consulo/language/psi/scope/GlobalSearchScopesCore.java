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

import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import org.jspecify.annotations.Nullable;

import java.util.*;

public class GlobalSearchScopesCore {
  public static GlobalSearchScope projectProductionScope(Project project) {
    return new ProductionScopeFilter(project);
  }

  public static GlobalSearchScope projectTestScope(Project project) {
    return new TestScopeFilter(project);
  }

  public static GlobalSearchScope directoryScope(PsiDirectory directory, boolean withSubdirectories) {
    return new DirectoryScope(directory, withSubdirectories);
  }

  public static GlobalSearchScope directoryScope(Project project, VirtualFile directory, boolean withSubdirectories) {
    return new DirectoryScope(project, directory, withSubdirectories);
  }

  public static GlobalSearchScope directoriesScope(Project project, boolean withSubdirectories, VirtualFile... directories) {
    if (directories.length == 1) {
      return directoryScope(project, directories[0], withSubdirectories);
    }
    BitSet withSubdirectoriesBS = new BitSet(directories.length);
    if (withSubdirectories) {
      withSubdirectoriesBS.set(0, directories.length);
    }
    return new DirectoriesScope(project, directories, withSubdirectoriesBS);
  }

  public static GlobalSearchScope filterScope(Project project, NamedScope set) {
    return new FilterScopeAdapter(project, set);
  }

  private static class FilterScopeAdapter extends GlobalSearchScope {
    private final NamedScope mySet;
    private final PsiManager myManager;

    private FilterScopeAdapter(Project project, NamedScope set) {
      super(project);
      mySet = set;
      myManager = PsiManager.getInstance(project);
    }

    @Override
    public boolean contains(VirtualFile file) {
      Project project = Objects.requireNonNull(getProject());
      NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(project);
      for (NamedScopesHolder holder : holders) {
        PackageSet packageSet = mySet.getValue();
        if (packageSet != null) {
          return packageSet.contains(file, project, holder);
        }
      }
      return false;
    }

    @Override
    public @Nullable Image getIcon() {
      return mySet.getIcon();
    }

    @Override
    public String getDisplayName() {
      return mySet.getName();
    }

    @Override
    public @Nullable Project getProject() {
      return super.getProject();
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
      return true; //TODO (optimization?)
    }

    @Override
    public boolean isSearchInLibraries() {
      return true; //TODO (optimization?)
    }
  }

  private static class ProductionScopeFilter extends GlobalSearchScope {
    private final ProjectFileIndex myFileIndex;

    private ProductionScopeFilter(Project project) {
      super(project);
      myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    }

    @Override
    public boolean contains(VirtualFile file) {
      return myFileIndex.isInSourceContent(file) && !myFileIndex.isInTestSourceContent(file);
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule, boolean testSources) {
      return !testSources;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }

    @Override
    public String getDisplayName() {
      return LanguageLocalize.psiSearchScopeProductionFiles().get();
    }
  }

  private static class TestScopeFilter extends GlobalSearchScope {
    private final Project myProject;

    private TestScopeFilter(Project project) {
      super(project);
      myProject = project;
    }

    @Override
    public boolean contains(VirtualFile file) {
      return TestSourcesFilter.isTestSources(file, myProject);
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule, boolean testSources) {
      return testSources;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }

    @Override
    public String getDisplayName() {
      return LanguageLocalize.psiSearchScopeTestFiles().get();
    }
  }

  private static class DirectoryScope extends GlobalSearchScope {
    private final VirtualFile myDirectory;
    private final boolean myWithSubdirectories;

    private DirectoryScope(PsiDirectory psiDirectory, boolean withSubdirectories) {
      super(psiDirectory.getProject());
      myWithSubdirectories = withSubdirectories;
      myDirectory = psiDirectory.getVirtualFile();
    }

    private DirectoryScope(Project project, VirtualFile directory, boolean withSubdirectories) {
      super(project);
      myWithSubdirectories = withSubdirectories;
      myDirectory = directory;
    }

    @Override
    public boolean contains(VirtualFile file) {
      return myWithSubdirectories ? VirtualFileUtil.isAncestor(myDirectory, file, false) : myDirectory.equals(file.getParent());
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }

    @Override
    public String toString() {
      //noinspection HardCodedStringLiteral
      return "directory scope: " + myDirectory + "; withSubdirs:" + myWithSubdirectories;
    }

    @Override
    public int hashCode() {
      return myDirectory.hashCode() * 31 + (myWithSubdirectories ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof DirectoryScope directoryScope
          && myDirectory.equals(directoryScope.myDirectory)
          && myWithSubdirectories == directoryScope.myWithSubdirectories;
    }

    @Override
    public GlobalSearchScope uniteWith(GlobalSearchScope scope) {
      if (equals(scope)) return this;
      if (scope instanceof DirectoryScope other) {
        VirtualFile otherDirectory = other.myDirectory;
        if (myWithSubdirectories && VirtualFileUtil.isAncestor(myDirectory, otherDirectory, false)) return this;
        if (other.myWithSubdirectories && VirtualFileUtil.isAncestor(otherDirectory, myDirectory, false)) return other;
        BitSet newWithSubdirectories = new BitSet();
        newWithSubdirectories.set(0, myWithSubdirectories);
        newWithSubdirectories.set(1, other.myWithSubdirectories);
        return new DirectoriesScope(getProject(), new VirtualFile[]{myDirectory, otherDirectory}, newWithSubdirectories);
      }
      return super.uniteWith(scope);
    }

    @Override
    public @Nullable Project getProject() {
      return super.getProject();
    }

    @Override
    public String getDisplayName() {
      return "Directory '" + myDirectory.getName() + "'";
    }
  }

  static class DirectoriesScope extends GlobalSearchScope {
    private final VirtualFile[] myDirectories;
    private final BitSet myWithSubdirectories;

    private DirectoriesScope(@Nullable Project project, VirtualFile[] directories, BitSet withSubdirectories) {
      super(project);
      myWithSubdirectories = withSubdirectories;
      myDirectories = directories;
      if (directories.length < 2) {
        throw new IllegalArgumentException("Expected >1 directories, but got: " + Arrays.asList(directories));
      }
    }

    @Override
    public boolean contains(VirtualFile file) {
      VirtualFile parent = file.getParent();
      return parent != null && in(parent);
    }

    private boolean in(VirtualFile parent) {
      for (int i = 0; i < myDirectories.length; i++) {
        VirtualFile directory = myDirectories[i];
        boolean withSubdirectories = myWithSubdirectories.get(i);
        if (withSubdirectories ? VirtualFileUtil.isAncestor(directory, parent, false) : directory.equals(parent)) return true;
      }
      return false;
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }

    @Override
    public String toString() {
      //noinspection HardCodedStringLiteral
      return "Directories scope: " + Arrays.asList(myDirectories);
    }

    @Override
    public int hashCode() {
      int result = 1;
      for (int i = 0; i < myDirectories.length; i++) {
        VirtualFile directory = myDirectories[i];
        boolean withSubdirectories = myWithSubdirectories.get(i);
        result = result * 31 + directory.hashCode() * 31 + (withSubdirectories ? 1 : 0);
      }
      return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof DirectoriesScope that
          && Arrays.equals(myDirectories, that.myDirectories)
          && myWithSubdirectories.equals(that.myWithSubdirectories);
    }

    @Override
    public GlobalSearchScope uniteWith(GlobalSearchScope scope) {
      if (equals(scope)) {
        return this;
      }
      if (scope instanceof DirectoryScope) {
        if (in(((DirectoryScope)scope).myDirectory)) {
          return this;
        }
        VirtualFile[] newDirectories = ArrayUtil.append(myDirectories, ((DirectoryScope)scope).myDirectory, VirtualFile.class);
        BitSet newWithSubdirectories = (BitSet)myWithSubdirectories.clone();
        newWithSubdirectories.set(myDirectories.length, ((DirectoryScope)scope).myWithSubdirectories);
        return new DirectoriesScope(getProject(), newDirectories, newWithSubdirectories);
      }
      if (scope instanceof DirectoriesScope) {
        DirectoriesScope other = (DirectoriesScope)scope;
        List<VirtualFile> newDirectories = new ArrayList<VirtualFile>(myDirectories.length + other.myDirectories.length);
        newDirectories.addAll(Arrays.asList(other.myDirectories));
        BitSet newWithSubdirectories = (BitSet)myWithSubdirectories.clone();
        VirtualFile[] directories = other.myDirectories;
        for (int i = 0; i < directories.length; i++) {
          VirtualFile otherDirectory = directories[i];
          if (!in(otherDirectory)) {
            newWithSubdirectories.set(newDirectories.size(), other.myWithSubdirectories.get(i));
            newDirectories.add(otherDirectory);
          }
        }
        return new DirectoriesScope(getProject(), newDirectories.toArray(new VirtualFile[newDirectories.size()]), newWithSubdirectories);
      }
      return super.uniteWith(scope);
    }

    @Override
    public @Nullable Project getProject() {
      return super.getProject();
    }

    @Override
    public String getDisplayName() {
      if (myDirectories.length == 1) {
        VirtualFile root = myDirectories[0];
        return "Directory '" + root.getName() + "'";
      }
      return "Directories " + StringUtil.join(myDirectories, file -> "'" + file.getName() + "'", ", ");
    }
  }
}
