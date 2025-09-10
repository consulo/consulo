// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PsiNavigationSupport {
  public static PsiNavigationSupport getInstance() {
    return Application.get().getInstance(PsiNavigationSupport.class);
  }

  @Nullable
  public abstract Navigatable getDescriptor(@Nonnull PsiElement element);

  @Nonnull
  public abstract Navigatable createNavigatable(@Nonnull Project project, @Nonnull VirtualFile vFile, int offset);

  @RequiredReadAction
  public abstract boolean canNavigate(@Nonnull PsiElement element);

  public abstract void navigateToDirectory(@Nonnull PsiDirectory psiDirectory, boolean requestFocus);

  @Deprecated
  @RequiredUIAccess
  public void openDirectoryInSystemFileManager(@Nonnull File file) {
    Platform.current().openDirectoryInFileManager(file, UIAccess.current());
  }
}
