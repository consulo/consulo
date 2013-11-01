/*
 * Copyright 2013 Consulo.org
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
package com.intellij.psi.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.ide.IconDescriptorUpdater;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.NativeFileIconUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.consulo.lang.LanguageElementIcons;
import org.consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 0:28/19.07.13
 */
public class DefaultIconDescriptorUpdater implements IconDescriptorUpdater {
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    if (element instanceof PsiDirectory) {
      final PsiDirectory psiDirectory = (PsiDirectory)element;
      final VirtualFile vFile = psiDirectory.getVirtualFile();
      final Project project = psiDirectory.getProject();
      boolean isArhiveSystem = vFile.getParent() == null && vFile.getFileSystem() instanceof ArchiveFileSystem;
      boolean isContentRoot = ProjectRootsUtil.isModuleContentRoot(vFile, project);
      ContentFolder contentFolder = ProjectRootsUtil.getContentFolderIfIs(vFile, project);

      Icon symbolIcon;
      if (isArhiveSystem) {
        symbolIcon = AllIcons.Nodes.PpJar;
      }
      else if (isContentRoot) {
        symbolIcon = AllIcons.Nodes.Module;
      }
      else if (contentFolder != null) {
        symbolIcon = contentFolder.getType().getIcon();
      }
      else if (PsiPackageManager.getInstance(project).findAnyPackage(psiDirectory) != null) {
        symbolIcon = ProjectRootsUtil.isInTestSource(psiDirectory) || ProjectRootsUtil.isInTestResource(psiDirectory)
                     ? AllIcons.Nodes.TestPackage
                     : AllIcons.Nodes.Package;
      }
      else {
        symbolIcon = AllIcons.Nodes.TreeClosed;
      }

      iconDescriptor.setMainIcon(symbolIcon);
    }
    else if (element instanceof PsiFile) {
      if(iconDescriptor.getMainIcon() != null) {
        return;
      }

      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (virtualFile != null) {
        iconDescriptor.setMainIcon(NativeFileIconUtil.INSTANCE.getIcon(virtualFile));
      }

      if (iconDescriptor.getMainIcon() == null) {
        final FileType fileType = ((PsiFile)element).getFileType();
        iconDescriptor.setMainIcon(fileType.getIcon());
      }
    }
    else {
      Icon languageElementIcon = LanguageElementIcons.INSTANCE.forLanguage(element.getLanguage());
      if(languageElementIcon == null) {
        return;
      }

      iconDescriptor.addLayerIcon(languageElementIcon);
    }
  }
}
