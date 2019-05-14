// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

/**
 * @author yole
 */
public abstract class PsiNavigationSupport {
  public static PsiNavigationSupport getInstance() {
    return ServiceManager.getService(PsiNavigationSupport.class);
  }

  @Nullable
  public abstract Navigatable getDescriptor(@Nonnull PsiElement element);

  @Nonnull
  public abstract Navigatable createNavigatable(@Nonnull Project project, @Nonnull VirtualFile vFile, int offset);

  public abstract boolean canNavigate(@Nonnull PsiElement element);

  public abstract void navigateToDirectory(@Nonnull PsiDirectory psiDirectory, boolean requestFocus);

  public abstract void openDirectoryInSystemFileManager(@Nonnull File file);
}
