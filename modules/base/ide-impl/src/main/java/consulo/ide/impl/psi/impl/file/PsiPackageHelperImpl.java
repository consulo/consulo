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

package consulo.ide.impl.psi.impl.file;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.UserHomeFileUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiPackageHelper;
import consulo.language.psi.PsiPackageManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class PsiPackageHelperImpl extends PsiPackageHelper {
  private final PsiPackageManager myPackageManager;

  @Inject
  public PsiPackageHelperImpl(PsiPackageManager packageManager) {
    myPackageManager = packageManager;
  }

  @Override
  @Nonnull
  public String getQualifiedName(@Nonnull final PsiDirectory directory, final boolean presentable) {
    if (presentable) {
      return UserHomeFileUtil.getLocationRelativeToUserHome(directory.getVirtualFile().getPresentableUrl());
    }
    return "";
  }

  @Override
  public PsiDirectoryContainer getDirectoryContainer(@Nonnull PsiDirectory directory) {
    return myPackageManager.findAnyPackage(directory);
  }

  @Override
  public boolean isPackage(PsiDirectory directory) {
    return myPackageManager.findAnyPackage(directory) != null;
  }
}
