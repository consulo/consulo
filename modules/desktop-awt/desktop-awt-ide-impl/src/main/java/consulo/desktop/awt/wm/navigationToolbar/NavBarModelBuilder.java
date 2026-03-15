// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a new model for navigation bar by taking a giving element and
 * traverse path to root adding each element to a model
 */
public abstract class NavBarModelBuilder {
  private static final NavBarModelBuilder INSTANCE = new NavBarModelBuilderImpl();

  
  public static NavBarModelBuilder getInstance() {
    return INSTANCE;
  }

  public List<Object> createModel(PsiElement psiElement, Set<VirtualFile> roots, @Nullable NavBarModelExtension ownerExtension) {
    List<Object> model = new ArrayList<>();
    traverseToRoot(psiElement, roots, model, ownerExtension);
    return model;
  }

  abstract void traverseToRoot(PsiElement psiElement, Set<VirtualFile> roots, List<Object> model, @Nullable NavBarModelExtension ownerExtension);
}

