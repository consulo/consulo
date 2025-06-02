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

/*
 * User: anna
 * Date: 23-Jan-2008
 */
package consulo.project.ui.view.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.*;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectViewInternalHelper;
import consulo.ui.ex.tree.TreeHelper;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public class BaseProjectViewDirectoryHelper {
  private static final Logger LOGGER = Logger.getInstance(BaseProjectViewDirectoryHelper.class);

  @Nullable
  @RequiredReadAction
  public static String getLocationString(@Nonnull PsiDirectory psiDirectory) {
    PsiPackage aPackage = PsiPackageManager.getInstance(psiDirectory.getProject()).findAnyPackage(psiDirectory);
    if (ProjectRootsUtil.isSourceRoot(psiDirectory) && aPackage != null) {
      return aPackage.getQualifiedName();
    }

    final VirtualFile directory = psiDirectory.getVirtualFile();
    final VirtualFile contentRootForFile = ProjectRootManager.getInstance(psiDirectory.getProject()).getFileIndex().getContentRootForFile(directory);
    if (Comparing.equal(contentRootForFile, psiDirectory)) {
      return VirtualFilePathUtil.toPresentableUrl(directory.getUrl());
    }
    return null;
  }

  @RequiredReadAction
  public static boolean isShowFQName(Project project, ViewSettings settings, Object parentValue, PsiDirectory value) {
    PsiPackage aPackage;
    return value != null &&
           !(parentValue instanceof Project) &&
           settings.isFlattenPackages() &&
           (aPackage = PsiPackageManager.getInstance(project).findAnyPackage(value)) != null &&
           !aPackage.getQualifiedName().isEmpty();
  }

  @Nonnull
  @RequiredReadAction
  public static String getNodeName(ViewSettings settings, Object parentValue, @Nonnull PsiDirectory directory) {
    Project project = directory.getProject();

    PsiPackage aPackage = PsiPackageManager.getInstance(project).findAnyPackage(directory);

    String name = directory.getName();
    VirtualFile dirFile = directory.getVirtualFile();
    if (dirFile.getFileSystem() instanceof ArchiveFileSystem && dirFile.getParent() == null) {
      VirtualFile virtualFileForArchive = ArchiveVfsUtil.getVirtualFileForArchive(dirFile);
      if (virtualFileForArchive != null) {
        name = virtualFileForArchive.getName();
      }
    }

    PsiPackage parentPackage;
    if (!ProjectRootsUtil.isSourceRoot(directory) && aPackage != null && !aPackage.getQualifiedName().isEmpty() && parentValue instanceof PsiDirectory) {

      parentPackage = PsiPackageManager.getInstance(project).findAnyPackage(((PsiDirectory)parentValue));
    }
    else if (ProjectRootsUtil.isSourceRoot(directory) && aPackage != null) {   //package prefix
      aPackage = null;
      parentPackage = null;
    }
    else {
      parentPackage = null;
    }

    return TreeViewUtil.getNodeName(settings, aPackage, parentPackage, name, isShowFQName(project, settings, parentValue, directory));
  }

  @RequiredReadAction
  public static boolean skipDirectory(PsiDirectory directory) {
    return PsiPackageManager.getInstance(directory.getProject()).findAnyPackage(directory) == null;
  }

  @RequiredReadAction
  public static boolean isEmptyMiddleDirectory(PsiDirectory directory, final boolean strictlyEmpty) {
    return PsiPackageManager.getInstance(directory.getProject()).findAnyPackage(directory) != null && PackageNodeUtil.isEmptyMiddlePackage(directory, null, strictlyEmpty);
  }

  public static boolean canRepresent(Object element, PsiDirectory directory) {
    if (element instanceof VirtualFile) {
      VirtualFile vFile = (VirtualFile)element;
      return Comparing.equal(directory.getVirtualFile(), vFile);
    }
    if (element instanceof PackageElement) {
      final PackageElement packageElement = (PackageElement)element;
      return Arrays.asList(packageElement.getPackage().getDirectories()).contains(directory);
    }
    return false;
  }

  @RequiredReadAction
  public static Collection<AbstractTreeNode> getDirectoryChildren(final PsiDirectory psiDirectory, final ViewSettings settings, final boolean withSubDirectories) {
    return TreeHelper.calculateYieldingToWriteAction(() -> doGetDirectoryChildren(psiDirectory, settings, withSubDirectories));
  }

  @RequiredReadAction
  public static Collection<AbstractTreeNode> doGetDirectoryChildren(final PsiDirectory psiDirectory, final ViewSettings settings, final boolean withSubDirectories) {
    final List<AbstractTreeNode> children = new ArrayList<>();
    final Project project = psiDirectory.getProject();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Module module = fileIndex.getModuleForFile(psiDirectory.getVirtualFile());
    final ModuleFileIndex moduleFileIndex = module == null ? null : ModuleRootManager.getInstance(module).getFileIndex();
    if (!settings.isFlattenPackages() || skipDirectory(psiDirectory)) {
      processPsiDirectoryChildren(psiDirectory, directoryChildrenInProject(psiDirectory, settings), children, fileIndex, null, settings, withSubDirectories);
    }
    else { // source directory in "flatten packages" mode
      final PsiDirectory parentDir = psiDirectory.getParentDirectory();
      if (parentDir == null || skipDirectory(parentDir) /*|| !rootDirectoryFound(parentDir)*/ && withSubDirectories) {
        addAllSubpackages(children, psiDirectory, moduleFileIndex, settings);
      }
      PsiDirectory[] subdirs = psiDirectory.getSubdirectories();
      for (PsiDirectory subdir : subdirs) {
        if (!skipDirectory(subdir)) {
          continue;
        }
        VirtualFile directoryFile = subdir.getVirtualFile();
        if (FileTypeRegistry.getInstance().isFileIgnored(directoryFile)) continue;

        if (withSubDirectories) {
          children.add(new PsiDirectoryNode(project, subdir, settings));
        }
      }
      processPsiDirectoryChildren(psiDirectory, psiDirectory.getFiles(), children, fileIndex, moduleFileIndex, settings, withSubDirectories);
    }
    return children;
  }

  public static List<VirtualFile> getTopLevelRoots(Project project) {
    List<VirtualFile> topLevelContentRoots = new ArrayList<>();
    ProjectRootManager prm = ProjectRootManager.getInstance(project);
    ProjectFileIndex index = prm.getFileIndex();

    for (VirtualFile root : prm.getContentRoots()) {
      VirtualFile parent = root.getParent();
      if (parent == null || !index.isInContent(parent)) {
        topLevelContentRoots.add(root);
      }
    }
    return topLevelContentRoots;
  }

  @RequiredReadAction
  private static PsiElement[] directoryChildrenInProject(PsiDirectory psiDirectory, final ViewSettings settings) {
    DirectoryIndex directoryIndex = DirectoryIndex.getInstance(psiDirectory.getProject());
    VirtualFile dir = psiDirectory.getVirtualFile();
    if (shouldBeShown(directoryIndex, dir, settings)) {
      final List<PsiElement> children = new ArrayList<>();
      psiDirectory.processChildren(element -> {
        if (shouldBeShown(directoryIndex, element.getVirtualFile(), settings)) {
          children.add(element);
        }
        return true;
      });
      return PsiUtilCore.toPsiElementArray(children);
    }

    PsiManager manager = psiDirectory.getManager();
    Set<PsiElement> directoriesOnTheWayToContentRoots = new HashSet<>();
    for (VirtualFile root : getTopLevelRoots(psiDirectory.getProject())) {
      VirtualFile current = root;
      while (current != null) {
        VirtualFile parent = current.getParent();

        if (Comparing.equal(parent, dir)) {
          final PsiDirectory psi = manager.findDirectory(current);
          if (psi != null) {
            directoriesOnTheWayToContentRoots.add(psi);
          }
        }
        current = parent;
      }
    }

    return PsiUtilBase.toPsiElementArray(directoriesOnTheWayToContentRoots);
  }

  private static boolean shouldBeShown(DirectoryIndex directoryIndex, VirtualFile dir, ViewSettings settings) {
    DirectoryInfo directoryInfo = directoryIndex.getInfoForFile(dir);
    if (directoryInfo.isInProject(dir)) return true;

    return Objects.equals(settings.getViewOption(ProjectViewInternalHelper.SHOW_EXCLUDED_FILES_KEY), Boolean.TRUE)
        && directoryInfo.isExcluded(dir);
  }

  // used only for non-flatten packages mode
  @RequiredReadAction
  public static void processPsiDirectoryChildren(final PsiDirectory psiDir,
                                                 PsiElement[] children,
                                                 List<AbstractTreeNode> container,
                                                 ProjectFileIndex projectFileIndex,
                                                 ModuleFileIndex moduleFileIndex,
                                                 ViewSettings viewSettings,
                                                 boolean withSubDirectories) {
    Project project = psiDir.getProject();
    for (PsiElement child : children) {
      if (!(child instanceof PsiFileSystemItem)) {
        LOGGER.error("Either PsiFile or PsiDirectory expected as a child of " + child.getParent() + ", but was " + child);
        continue;
      }
      final VirtualFile vFile = ((PsiFileSystemItem)child).getVirtualFile();
      if (vFile == null) {
        continue;
      }
      if (moduleFileIndex != null && !moduleFileIndex.isInContent(vFile)) {
        continue;
      }

      if (child instanceof PsiFile) {
        container.add(new PsiFileNode(project, (PsiFile)child, viewSettings));
      }
      else if (child instanceof PsiDirectory) {
        if (withSubDirectories) {
          PsiDirectory dir = (PsiDirectory)child;
          if (!vFile.equals(projectFileIndex.getSourceRootForFile(vFile))) { // if is not a source root
            if (viewSettings.isHideEmptyMiddlePackages() && !skipDirectory(psiDir) && isEmptyMiddleDirectory(dir, true)) {
              processPsiDirectoryChildren(dir, directoryChildrenInProject(dir, viewSettings), container, projectFileIndex, moduleFileIndex, viewSettings, withSubDirectories); // expand it recursively
              continue;
            }
          }

          container.add(new PsiDirectoryNode(project, dir, viewSettings));
        }
      }
    }
  }

  // used only in flatten packages mode
  @RequiredReadAction
  public static void addAllSubpackages(List<AbstractTreeNode> container, PsiDirectory dir, ModuleFileIndex moduleFileIndex, ViewSettings viewSettings) {
    final Project project = dir.getProject();
    PsiDirectory[] subdirs = dir.getSubdirectories();
    for (PsiDirectory subdir : subdirs) {
      if (skipDirectory(subdir)) {
        continue;
      }
      if (moduleFileIndex != null) {
        if (!moduleFileIndex.isInContent(subdir.getVirtualFile())) {
          container.add(new PsiDirectoryNode(project, subdir, viewSettings));
          continue;
        }
      }
      if (viewSettings.isHideEmptyMiddlePackages()) {
        if (!isEmptyMiddleDirectory(subdir, false)) {

          container.add(new PsiDirectoryNode(project, subdir, viewSettings));
        }
      }
      else {
        container.add(new PsiDirectoryNode(project, subdir, viewSettings));
      }
      addAllSubpackages(container, subdir, moduleFileIndex, viewSettings);
    }
  }
}
