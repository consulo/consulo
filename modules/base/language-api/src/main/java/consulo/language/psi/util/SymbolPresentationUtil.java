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
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public class SymbolPresentationUtil {
  private SymbolPresentationUtil() {
  }

  @RequiredReadAction
  public static String getSymbolPresentableText(@Nonnull PsiElement element) {
    if (element instanceof NavigationItem) {
      final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
      if (presentation != null){
        return presentation.getPresentableText();
      }
    }

    if (element instanceof PsiNamedElement) return ((PsiNamedElement)element).getName();
    return element.getText();
  }

  @Nullable
  @RequiredReadAction
  public static String getSymbolContainerText(PsiElement element) {
    if (element instanceof NavigationItem) {
      final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
      if (presentation != null){
        return presentation.getLocationString();
      } else {
        PsiFile file = element.getContainingFile();
        if (file != null) {
          VirtualFile virtualFile = file.getVirtualFile();
          if (virtualFile != null) return virtualFile.getPath();
        }
      }
    }

    return null;
  }

  @RequiredReadAction
  public static String getFilePathPresentation(PsiFile psiFile) {
    ProjectFileIndex index = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
    VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
    VirtualFile rootForFile = file != null ? index.getContentRootForFile(file):null;

    if (rootForFile != null) {
      String relativePath = VirtualFileUtil.getRelativePath(file, rootForFile, File.separatorChar);
      if (relativePath != null) return relativePath;
    }

    return psiFile.getName();
  }
}