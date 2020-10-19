/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.image.Image;
import consulo.vfs.ArchiveFileSystem;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Jan-17
 */
public class PsiDirectoryIconDescriptorUpdater implements IconDescriptorUpdater {
  private final ProjectFileIndex myProjectFileIndex;
  private final ProjectRootManager myProjectRootManager;
  private final PsiPackageManager myPsiPackageManager;

  @Inject
  public PsiDirectoryIconDescriptorUpdater(ProjectFileIndex projectFileIndex, ProjectRootManager projectRootManager, PsiPackageManager psiPackageManager) {
    myProjectFileIndex = projectFileIndex;
    myProjectRootManager = projectRootManager;
    myPsiPackageManager = psiPackageManager;
  }

  @RequiredReadAction
  @Override
  public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
    if (element instanceof PsiDirectory) {
      PsiDirectory psiDirectory = (PsiDirectory)element;
      VirtualFile virtualFile = psiDirectory.getVirtualFile();

      Image symbolIcon;
      if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
        if (virtualFile.getParent() == null) {
          symbolIcon = AllIcons.Nodes.PpJar;
        }
        else {
          PsiPackage psiPackage = myPsiPackageManager.findAnyPackage(virtualFile);
          symbolIcon = psiPackage != null ? AllIcons.Nodes.Package : AllIcons.Nodes.TreeClosed;
        }
      }
      else if (ProjectRootsUtil.isModuleContentRoot(myProjectFileIndex, virtualFile)) {
        symbolIcon = AllIcons.Nodes.Module;
      }
      else {
        boolean ignored = myProjectRootManager.getFileIndex().isExcluded(virtualFile);
        if (ignored) {
          symbolIcon = AllIcons.Modules.ExcludeRoot;
        }
        else {
          ContentFolder contentFolder = ProjectRootsUtil.findContentFolderForDirectory(myProjectFileIndex, virtualFile);
          if (contentFolder != null) {
            symbolIcon = contentFolder.getType().getIcon(contentFolder.getProperties());
          }
          else {
            ContentFolderTypeProvider contentFolderTypeForFile = myProjectFileIndex.getContentFolderTypeForFile(virtualFile);
            symbolIcon = contentFolderTypeForFile != null ? contentFolderTypeForFile.getChildDirectoryIcon(psiDirectory, myPsiPackageManager) : AllIcons.Nodes.TreeClosed;
          }
        }
      }

      if (symbolIcon != null) {
        iconDescriptor.setMainIcon(symbolIcon);
      }

      if (virtualFile.is(VFileProperty.SYMLINK)) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
      }
    }
    else if (element instanceof PsiPackage) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Package);
    }
  }
}
