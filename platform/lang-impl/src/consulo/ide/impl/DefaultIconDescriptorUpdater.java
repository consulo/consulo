/*
 * Copyright 2013-2016 consulo.io
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
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.NativeFileIconUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import consulo.vfs.ArchiveFileSystem;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;
import consulo.annotations.RequiredReadAction;
import consulo.lang.LanguageElementIcons;
import consulo.roots.ContentFolderTypeProvider;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:28/19.07.13
 */
public class DefaultIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof PsiDirectory) {
      PsiDirectory psiDirectory = (PsiDirectory)element;
      VirtualFile virtualFile = psiDirectory.getVirtualFile();
      if(iconDescriptor.getMainIcon() == null) {
        Project project = psiDirectory.getProject();

        Icon symbolIcon;
        if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
          if (virtualFile.getParent() == null) {
            symbolIcon = AllIcons.Nodes.PpJar;
          }
          else {
            PsiPackage psiPackage = PsiPackageManager.getInstance(project).findAnyPackage(psiDirectory);
            symbolIcon = psiPackage != null ? AllIcons.Nodes.Package : AllIcons.Nodes.TreeClosed;
          }
        }
        else if (ProjectRootsUtil.isModuleContentRoot(virtualFile, project)) {
          symbolIcon = AllIcons.Nodes.Module;
        }
        else {
          boolean ignored = ProjectRootManager.getInstance(project).getFileIndex().isExcluded(virtualFile);
          if (ignored) {
            symbolIcon = AllIcons.Modules.ExcludeRoot;
          }
          else {
            ContentFolder contentFolder = ProjectRootsUtil.getContentFolderIfIs(virtualFile, project);
            if (contentFolder != null) {
              symbolIcon = contentFolder.getType().getIcon(contentFolder.getProperties());
            }
            else {
              ContentFolderTypeProvider contentFolderTypeForFile = ProjectFileIndex.SERVICE.getInstance(project).getContentFolderTypeForFile(virtualFile);
              symbolIcon = contentFolderTypeForFile != null ? contentFolderTypeForFile.getChildDirectoryIcon(psiDirectory) : AllIcons.Nodes.TreeClosed;
            }
          }
        }

        iconDescriptor.setMainIcon(symbolIcon);      }

      if(virtualFile.is(VFileProperty.SYMLINK)) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
      }
    }
    else if (element instanceof PsiPackage) {
      iconDescriptor.setMainIcon(AllIcons.Nodes.Package);
    }
    else if (element instanceof PsiFile) {
      if (iconDescriptor.getMainIcon() == null) {
        VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        if (virtualFile != null) {
          iconDescriptor.setMainIcon(NativeFileIconUtil.INSTANCE.getIcon(virtualFile));
        }

        if (iconDescriptor.getMainIcon() == null) {
          FileType fileType = ((PsiFile)element).getFileType();
          iconDescriptor.setMainIcon(fileType.getIcon());
        }
      }

      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if(virtualFile != null && virtualFile.is(VFileProperty.SYMLINK)) {
        iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
      }
    }
    else {
      Icon languageElementIcon = LanguageElementIcons.INSTANCE.forLanguage(element.getLanguage());
      if (languageElementIcon == null) {
        return;
      }

      iconDescriptor.addLayerIcon(languageElementIcon);
    }
  }
}
