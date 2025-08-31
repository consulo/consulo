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
package consulo.project.ui.view.tree;

import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public class PackageNodeUtil {
  /**
   * a directory is considered "empty" if it has at least one child and all its children are only directories
   *
   * @param strictlyEmpty if true, the package is considered empty if it has only 1 child and this child  is a directory
   *                      otherwise the package is considered as empty if all direct children that it has are directories
   */
  public static boolean isEmptyMiddlePackage(@Nonnull PsiDirectory dir, @Nullable Class<? extends ModuleExtension> moduleExtensionClass, boolean strictlyEmpty) {
    VirtualFile[] files = dir.getVirtualFile().getChildren();
    if (files.length == 0) {
      return false;
    }
    PsiManager manager = dir.getManager();
    int subpackagesCount = 0;
    int directoriesCount = 0;
    for (VirtualFile file : files) {
      if (FileTypeManager.getInstance().isFileIgnored(file)) continue;
      if (!file.isDirectory()) return false;
      PsiDirectory childDir = manager.findDirectory(file);
      if (childDir != null) {
        directoriesCount++;
        if (strictlyEmpty && directoriesCount > 1) return false;

        PsiPackageManager psiPackageManager = PsiPackageManager.getInstance(dir.getProject());
        PsiPackage tempPackage = moduleExtensionClass == null ? psiPackageManager.findAnyPackage(childDir) : psiPackageManager.findPackage(dir, moduleExtensionClass);
        if (tempPackage != null) {
          subpackagesCount++;
        }
      }
    }
    if (strictlyEmpty) {
      return directoriesCount == subpackagesCount && directoriesCount == 1;
    }
    return directoriesCount == subpackagesCount && directoriesCount > 0;
  }

  private static class ModuleLibrariesSearchScope extends GlobalSearchScope {
    private final Module myModule;

    public ModuleLibrariesSearchScope(@Nonnull Module module) {
      super(module.getProject());
      myModule = module;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      OrderEntry orderEntry = ModuleRootManager.getInstance(myModule).getFileIndex().getOrderEntryForFile(file);
      return orderEntry instanceof ModuleExtensionWithSdkOrderEntry || orderEntry instanceof LibraryOrderEntry;
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      ModuleFileIndex fileIndex = ModuleRootManager.getInstance(myModule).getFileIndex();
      return Comparing.compare(fileIndex.getOrderEntryForFile(file2), fileIndex.getOrderEntryForFile(file1));
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return false;
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }
  }

  private static class ProjectLibrariesSearchScope extends GlobalSearchScope {
    private final DirectoryIndex myDirectoryIndex;

    public ProjectLibrariesSearchScope(@Nonnull Project project) {
      super(project);
      myDirectoryIndex = DirectoryIndex.getInstance(project);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      VirtualFile dir = file.isDirectory() ? file : file.getParent();
      if (dir == null) return false;

      DirectoryInfo info = myDirectoryIndex.getInfoForDirectory(dir);
      return info != null && info.hasLibraryClassRoot();
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      throw new IncorrectOperationException("not implemented");
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return false;
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }
  }

  public static boolean isPackageEmpty(@Nonnull PsiPackage aPackage, @Nullable Module module, boolean strictlyEmpty, boolean inLibrary) {
    Project project = aPackage.getProject();
    PsiDirectory[] dirs = getDirectories(aPackage, project, module, inLibrary);
    for (PsiDirectory dir : dirs) {
      if (!isEmptyMiddlePackage(dir, null, strictlyEmpty)) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  public static Collection<AbstractTreeNode> createPackageViewChildrenOnFiles(@Nonnull List<VirtualFile> sourceRoots,
                                                                              @Nonnull Project project,
                                                                              @Nonnull ViewSettings settings,
                                                                              @Nullable Module module,
                                                                              boolean inLibrary) {
    PsiManager psiManager = PsiManager.getInstance(project);

    List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    Set<PsiPackage> topLevelPackages = new HashSet<PsiPackage>();

    for (VirtualFile root : sourceRoots) {
      PsiDirectory directory = psiManager.findDirectory(root);
      if (directory == null) {
        continue;
      }
      PsiPackage directoryPackage = PsiPackageManager.getInstance(project).findAnyPackage(directory);
      if (directoryPackage == null || isPackageDefault(directoryPackage)) {
        // add subpackages
        PsiDirectory[] subdirectories = directory.getSubdirectories();
        for (PsiDirectory subdirectory : subdirectories) {
          PsiPackage aPackage = PsiPackageManager.getInstance(project).findAnyPackage(subdirectory);
          if (aPackage != null && !isPackageDefault(aPackage)) {
            topLevelPackages.add(aPackage);
          }
        }
        // add non-dir items
        children.addAll(BaseProjectViewDirectoryHelper.getDirectoryChildren(directory, settings, false));
      }
      else {
        topLevelPackages.add(directoryPackage);
      }
    }

    for (PsiPackage topLevelPackage : topLevelPackages) {
      addPackageAsChild(children, topLevelPackage, module, settings, inLibrary);
    }

    return children;
  }

  public static boolean isPackageDefault(@Nonnull PsiPackage directoryPackage) {
    String qName = directoryPackage.getQualifiedName();
    return qName.isEmpty();
  }

  public static void addPackageAsChild(@Nonnull Collection<AbstractTreeNode> children, @Nonnull PsiPackage aPackage, @Nullable Module module, @Nonnull ViewSettings settings, boolean inLibrary) {
    boolean shouldSkipPackage = settings.isHideEmptyMiddlePackages() && isPackageEmpty(aPackage, module, !settings.isFlattenPackages(), inLibrary);
    Project project = aPackage.getProject();
    if (!shouldSkipPackage) {
      children.add(new PackageElementNode(project, new PackageElement(module, aPackage, inLibrary), settings));
    }
    if (settings.isFlattenPackages() || shouldSkipPackage) {
      PsiPackage[] subpackages = getSubpackages(aPackage, module, project, inLibrary);
      for (PsiPackage subpackage : subpackages) {
        addPackageAsChild(children, subpackage, module, settings, inLibrary);
      }
    }
  }

  @Nonnull
  public static PsiPackage[] getSubpackages(@Nonnull PsiPackage aPackage, @Nullable Module module, @Nonnull Project project, boolean searchInLibraries) {
    PsiDirectory[] dirs = getDirectories(aPackage, project, module, searchInLibraries);
    Set<PsiPackage> subpackages = new HashSet<PsiPackage>();
    for (PsiDirectory dir : dirs) {
      PsiDirectory[] subdirectories = dir.getSubdirectories();
      for (PsiDirectory subdirectory : subdirectories) {
        PsiPackage psiPackage = PsiPackageManager.getInstance(project).findAnyPackage(subdirectory);
        if (psiPackage != null) {
          String name = psiPackage.getName();
          // skip "default" subpackages as they should be attributed to other modules
          // this is the case when contents of one module is nested into contents of another
          if (name != null && !name.isEmpty()) {
            subpackages.add(psiPackage);
          }
        }
      }
    }
    return subpackages.toArray(new PsiPackage[subpackages.size()]);
  }

  @Nonnull
  public static PsiDirectory[] getDirectories(@Nonnull PsiPackage aPackage, @Nonnull Project project, @Nullable Module module, boolean inLibrary) {
    GlobalSearchScope scopeToShow = getScopeToShow(project, module, inLibrary);
    return aPackage.getDirectories(scopeToShow);
  }

  @Nonnull
  private static GlobalSearchScope getScopeToShow(@Nonnull Project project, @Nullable Module module, boolean forLibraries) {
    if (module == null) {
      if (forLibraries) {
        return new ProjectLibrariesSearchScope(project);
      }
      return GlobalSearchScope.projectScope(project);
    }
    else {
      if (forLibraries) {
        return new ModuleLibrariesSearchScope(module);
      }
      return GlobalSearchScope.moduleScope(module);
    }
  }
}
