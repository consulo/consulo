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
package consulo.roots;

import com.intellij.psi.PsiDirectory;
import consulo.annotation.access.RequiredReadAction;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.ui.image.Image;

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
  public final Image getChildDirectoryIcon(@Nullable PsiDirectory psiDirectory) {
    return getChildDirectoryIcon(psiDirectory, null);
  }

  @Nonnull
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable PsiDirectory psiDirectory, @Nullable PsiPackageManager oldPsiPackageManager) {
    Image packageIcon = getChildPackageIcon();
    if (packageIcon == null) {
      return getChildDirectoryIcon();
    }

    if (psiDirectory != null) {
      PsiPackageManager psiPackageManager = oldPsiPackageManager == null ? PsiPackageManager.getInstance(psiDirectory.getProject()) : oldPsiPackageManager;
      PsiPackage anyPackage = psiPackageManager.findAnyPackage(psiDirectory);
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
