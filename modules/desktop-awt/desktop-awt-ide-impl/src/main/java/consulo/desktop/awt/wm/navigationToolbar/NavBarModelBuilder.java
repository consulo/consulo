// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a new model for navigation bar by taking a giving element and
 * traverse path to root adding each element to a model
 */
public abstract class NavBarModelBuilder {
  private static final NavBarModelBuilder INSTANCE = new NavBarModelBuilderImpl();

  @Nonnull
  public static NavBarModelBuilder getInstance() {
    return INSTANCE;
  }

  public List<Object> createModel(@Nonnull PsiElement psiElement, @Nonnull Set<VirtualFile> roots, @Nullable NavBarModelExtension ownerExtension) {
    final List<Object> model = new ArrayList<>();
    traverseToRoot(psiElement, roots, model, ownerExtension);
    return model;
  }

  abstract void traverseToRoot(@Nonnull PsiElement psiElement, @Nonnull Set<VirtualFile> roots, @Nonnull List<Object> model, @Nullable NavBarModelExtension ownerExtension);
}

