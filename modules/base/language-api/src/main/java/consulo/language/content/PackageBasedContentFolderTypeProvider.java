/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.content;

import consulo.language.psi.PsiDirectory;
import consulo.annotation.access.RequiredReadAction;
import consulo.component.ComponentManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.project.Project;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20/01/2022
 */
public abstract class PackageBasedContentFolderTypeProvider extends ContentFolderTypeProvider {
  public PackageBasedContentFolderTypeProvider(String id) {
    super(id);
  }

  /**
   * Return child directory icon
   * If psiDirectory is null it require force package support if this provider is supported it
   *
   * @param psiDirectory child directory
   * @return icon of child directory
   */
  @Override
  @Nonnull
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable VirtualFile file, @Nullable ComponentManager project) {
    return getChildDirectoryIcon(file, project, null);
  }

  @Nonnull
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable PsiDirectory psiDirectory, @Nullable PsiPackageManager oldPsiPackageManager) {
    Project project = psiDirectory == null ? null : psiDirectory.getProject();
    VirtualFile virtualFile = psiDirectory == null ? null : psiDirectory.getVirtualFile();
    return getChildDirectoryIcon(virtualFile, project, oldPsiPackageManager);
  }

  @Nonnull
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable VirtualFile file, @Nullable ComponentManager project, @Nullable PsiPackageManager oldPsiPackageManager) {
    Image packageIcon = getChildPackageIcon();
    if (packageIcon == null) {
      return getChildDirectoryIcon();
    }

    if (file != null && project != null) {
      PsiPackageManager psiPackageManager = oldPsiPackageManager == null ? PsiPackageManager.getInstance((Project)project) : oldPsiPackageManager;
      PsiPackage anyPackage = psiPackageManager.findAnyPackage(file);
      if (anyPackage != null) {
        return packageIcon;
      }
      else {
        return getChildDirectoryIcon();
      }
    }
    else {
      return packageIcon;
    }
  }
}
