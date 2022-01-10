/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.todo.nodes;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.nodes.PackageNodeUtil;
import com.intellij.ide.todo.TodoFileDirAndModuleComparator;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ide.projectView.impl.nodes.PackageElement;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.roots.ContentFolderScopes;

import javax.annotation.Nullable;
import java.util.*;

/**
 * User: anna
 * Date: May 27, 2005
 */
public class TodoTreeHelper {
  public static void addPackagesToChildren(final ArrayList<AbstractTreeNode> children, Project project, @Nullable Module module, final TodoTreeBuilder builder) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
    if (module == null) {
      final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
      ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());
    }
    else {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      ContainerUtil.addAll(sourceRoots, moduleRootManager.getContentFolderFiles(ContentFolderScopes.productionAndTest()));
    }

    final Set<PsiPackage> topLevelPackages = new HashSet<PsiPackage>();
    for (final VirtualFile root : sourceRoots) {
      final PsiDirectory directory = psiManager.findDirectory(root);
      if (directory == null) {
        continue;
      }
      final PsiPackage directoryPackage = PsiPackageManager.getInstance(project).findAnyPackage(directory);
      if (directoryPackage == null || PackageNodeUtil.isPackageDefault(directoryPackage)) {
        // add subpackages
        final PsiDirectory[] subdirectories = directory.getSubdirectories();
        for (PsiDirectory subdirectory : subdirectories) {
          final PsiPackage aPackage = PsiPackageManager.getInstance(project).findAnyPackage(subdirectory);
          if (aPackage != null && !PackageNodeUtil.isPackageDefault(aPackage)) {
            topLevelPackages.add(aPackage);
          }
          else {
            final Iterator<PsiFile> files = builder.getFiles(subdirectory);
            if (!files.hasNext()) continue;
            TodoDirNode dirNode = new TodoDirNode(project, subdirectory, builder);
            if (!children.contains(dirNode)) {
              children.add(dirNode);
            }
          }
        }
        // add non-dir items
        final Iterator<PsiFile> filesUnderDirectory = builder.getFilesUnderDirectory(directory);
        for (; filesUnderDirectory.hasNext(); ) {
          final PsiFile file = filesUnderDirectory.next();
          TodoFileNode todoFileNode = new TodoFileNode(project, file, builder, false);
          if (!children.contains(todoFileNode)) {
            children.add(todoFileNode);
          }
        }
      }
      else {
        // this is the case when a source root has pakage prefix assigned
        topLevelPackages.add(directoryPackage);
      }
    }

    GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(project);
    ArrayList<PsiPackage> packages = new ArrayList<PsiPackage>();
    for (PsiPackage psiPackage : topLevelPackages) {
      final PsiPackage aPackage = findNonEmptyPackage(psiPackage, module, project, builder, scope);
      if (aPackage != null) {
        packages.add(aPackage);
      }
    }
    for (PsiPackage psiPackage : packages) {
      if (!builder.getTodoTreeStructure().getIsFlattenPackages()) {
        PackageElement element =
          new PackageElement(module, psiPackage, false);
        TodoPackageNode packageNode = new TodoPackageNode(project, element, builder, psiPackage.getQualifiedName());
        if (!children.contains(packageNode)) {
          children.add(packageNode);
        }
      }
      else {
        Set<PsiPackage> allPackages = new HashSet<PsiPackage>();
        traverseSubPackages(psiPackage, module, builder, project, allPackages);
        for (PsiPackage aPackage : allPackages) {
          TodoPackageNode packageNode =
            new TodoPackageNode(project, new PackageElement(module, aPackage, false), builder);
          if (!children.contains(packageNode)) {
            children.add(packageNode);
          }
        }
      }
    }
    addPackagesToChildren0(project, children, module, builder);
  }

  private static void addPackagesToChildren0(Project project, ArrayList<AbstractTreeNode> children, Module module, TodoTreeBuilder builder) {
    final List<VirtualFile> roots = new ArrayList<VirtualFile>();
    final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
    final PsiManager psiManager = PsiManager.getInstance(project);
    if (module == null) {
      final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
      ContainerUtil.addAll(roots, projectRootManager.getContentRoots());
      ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());
    }
    else {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      ContainerUtil.addAll(roots, moduleRootManager.getContentRoots());
      ContainerUtil.addAll(sourceRoots, moduleRootManager.getContentFolderFiles(ContentFolderScopes.productionAndTest()));
    }
    roots.removeAll(sourceRoots);
    for (VirtualFile dir : roots) {
      final PsiDirectory directory = psiManager.findDirectory(dir);
      if (directory == null) {
        continue;
      }
      final Iterator<PsiFile> files = builder.getFiles(directory);
      if (!files.hasNext()) continue;
      TodoDirNode dirNode = new TodoDirNode(project, directory, builder);
      if (!children.contains(dirNode)) {
        children.add(dirNode);
      }
    }
  }

  @Nullable
  public static PsiPackage findNonEmptyPackage(PsiPackage rootPackage,
                                                   Module module,
                                                   Project project,
                                                   TodoTreeBuilder builder,
                                                   GlobalSearchScope scope) {
    if (!isPackageEmpty(new PackageElement(module, rootPackage, false), builder, project)) {
      return rootPackage;
    }
    final PsiPackage[] subPackages = rootPackage.getSubPackages(scope);
    PsiPackage suggestedNonEmptyPackage = null;
    int count = 0;
    for (PsiPackage aPackage : subPackages) {
      if (!isPackageEmpty(new PackageElement(module, aPackage, false), builder, project)) {
        if (++count > 1) return rootPackage;
        suggestedNonEmptyPackage = aPackage;
      }
    }
    for (PsiPackage aPackage : subPackages) {
      if (aPackage != suggestedNonEmptyPackage) {
        PsiPackage subPackage = findNonEmptyPackage(aPackage, module, project, builder, scope);
        if (subPackage != null) {
          if (count > 0) {
            return rootPackage;
          }
          else {
            count++;
            suggestedNonEmptyPackage = subPackage;
          }
        }
      }
    }
    return suggestedNonEmptyPackage;
  }

  private static void traverseSubPackages(PsiPackage psiPackage,
                                          Module module,
                                          TodoTreeBuilder builder,
                                          Project project,
                                          Set<PsiPackage> packages) {
    if (!isPackageEmpty(new PackageElement(module, psiPackage, false), builder, project)) {
      packages.add(psiPackage);
    }
    GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(project);
    final PsiPackage[] subPackages = psiPackage.getSubPackages(scope);
    for (PsiPackage subPackage : subPackages) {
      traverseSubPackages(subPackage, module, builder, project, packages);
    }
  }

  private static boolean isPackageEmpty(PackageElement packageElement,
                                        TodoTreeBuilder builder,
                                        Project project) {
    if (packageElement == null) return true;
    final PsiPackage psiPackage = packageElement.getPackage();
    final Module module = packageElement.getModule();
    GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(project);
    final PsiDirectory[] directories = psiPackage.getDirectories(scope);
    boolean isEmpty = true;
    for (PsiDirectory psiDirectory : directories) {
      isEmpty &= builder.isDirectoryEmpty(psiDirectory);
    }
    return isEmpty;
  }

  public static Collection<AbstractTreeNode> getDirectoryChildren(PsiDirectory psiDirectory, TodoTreeBuilder builder, boolean isFlatten) {
    final Project project = psiDirectory.getProject();
    ArrayList<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    if (!isFlatten || !skipDirectory(psiDirectory)) {
      final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
      while (iterator.hasNext()) {
        final PsiFile psiFile = iterator.next();
        // Add files
        final PsiDirectory containingDirectory = psiFile.getContainingDirectory();
        TodoFileNode todoFileNode = new TodoFileNode(project, psiFile, builder, false);
        if (psiDirectory.equals(containingDirectory) && !children.contains(todoFileNode)) {
          children.add(todoFileNode);
          continue;
        }
        // Add directories (find first ancestor directory that is in our psiDirectory)
        PsiDirectory _dir = psiFile.getContainingDirectory();
        while (_dir != null) {
          if (skipDirectory(_dir)) {
            break;
          }
          final PsiDirectory parentDirectory = _dir.getParentDirectory();
          TodoDirNode todoDirNode = new TodoDirNode(project, _dir, builder);
          if (parentDirectory != null && psiDirectory.equals(parentDirectory) && !children.contains(todoDirNode)) {
            children.add(todoDirNode);
            break;
          }
          _dir = parentDirectory;
        }
      }
    }
    else { // flatten packages
      final PsiDirectory parentDirectory = psiDirectory.getParentDirectory();
      if (parentDirectory == null ||
          !skipDirectory(parentDirectory) ||
          !ProjectRootManager.getInstance(project).getFileIndex().isInContent(parentDirectory.getVirtualFile())) {
        final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
        while (iterator.hasNext()) {
          final PsiFile psiFile = iterator.next();
          // Add files
          TodoFileNode todoFileNode = new TodoFileNode(project, psiFile, builder, false);
          if (psiDirectory.equals(psiFile.getContainingDirectory()) && !children.contains(todoFileNode)) {
            children.add(todoFileNode);
            continue;
          }
          // Add directories
          final PsiDirectory _dir = psiFile.getContainingDirectory();
          if (skipDirectory(_dir)) {
            continue;
          }
          TodoDirNode todoDirNode = new TodoDirNode(project, _dir, builder);
          if (PsiTreeUtil.isAncestor(psiDirectory, _dir, true) && !children.contains(todoDirNode) && !builder.isDirectoryEmpty(_dir)) {
            children.add(todoDirNode);
          }
        }
      }
      else {
        final Iterator<PsiFile> iterator = builder.getFiles(psiDirectory);
        while (iterator.hasNext()) {
          final PsiFile psiFile = iterator.next();
          final PsiDirectory containingDirectory = psiFile.getContainingDirectory();
          TodoFileNode todoFileNode = new TodoFileNode(project, psiFile, builder, false);
          if (psiDirectory.equals(containingDirectory) && !children.contains(todoFileNode)) {
            children.add(todoFileNode);
          }
        }
      }
    }
    Collections.sort(children, TodoFileDirAndModuleComparator.INSTANCE);
    return children;
  }

  public static boolean skipDirectory(final PsiDirectory directory) {
    return PsiPackageManager.getInstance(directory.getProject()).findAnyPackage(directory) != null;
  }

  @Nullable
  public static PsiElement getSelectedElement(Object userObject) {
    if (userObject instanceof TodoDirNode) {
      TodoDirNode descriptor = (TodoDirNode)userObject;
      return descriptor.getValue();
    }
    else if (userObject instanceof TodoFileNode) {
      TodoFileNode descriptor = (TodoFileNode)userObject;
      return descriptor.getValue();
    }
    else if (userObject instanceof TodoPackageNode) {
      TodoPackageNode descriptor = (TodoPackageNode)userObject;
      final PackageElement packageElement = descriptor.getValue();
      return packageElement != null ? packageElement.getPackage() : null;
    }
    return null;
  }

  public static boolean contains(ProjectViewNode node, Object element) {
    if (element instanceof PackageElement) {
      for (VirtualFile virtualFile : ((PackageElement)element).getRoots()) {
        if (node.contains(virtualFile)) return true;
      }
    }
    return false;
  }
}
