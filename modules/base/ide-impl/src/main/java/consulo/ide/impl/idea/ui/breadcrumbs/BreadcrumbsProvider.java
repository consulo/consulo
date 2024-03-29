// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexey.Pegov
 * @author Sergey.Malenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BreadcrumbsProvider {
  ExtensionPointName<BreadcrumbsProvider> EP_NAME = ExtensionPointName.create(BreadcrumbsProvider.class);

  /**
   * @return an array of languages supported by this provider
   */
  @Nonnull
  default Language[] getLanguages() {
    return new Language[]{getLanguage()};
  }

  @Nonnull
  default Language getLanguage() {
    throw new AbstractMethodError();
  }

  /**
   * @param element that represents a single crumb
   * @return {@code true} if the specified element is supported by this provider
   */
  boolean acceptElement(@Nonnull PsiElement element);

  /**
   * @param element that represents a single crumb
   * @return a text for the specified element
   */
  @Nonnull
  String getElementInfo(@Nonnull PsiElement element);

  /**
   * @param element that represents a single crumb
   * @return an icon for the specified element
   */
  @Nullable
  default Image getElementIcon(@Nonnull PsiElement element) {
    return null;
  }

  /**
   * @param element that represents a single crumb
   * @return a description for the specified element
   */
  @Nullable
  default String getElementTooltip(@Nonnull PsiElement element) {
    return null;
  }

  /**
   * @param element that represents a single crumb
   * @return an element that represents a parent crumb, or {@code null}
   */
  @Nullable
  default PsiElement getParent(@Nonnull PsiElement element) {
    return element.getParent();
  }

  /**
   * Reserved for future releases. Not supported yet.
   *
   * @param element that represents a single crumb
   * @return a list of elements to navigate
   */
  @Nonnull
  default List<PsiElement> getChildren(@Nonnull PsiElement element) {
    return Collections.emptyList();
  }

  /**
   * @param element that represents a single crumb
   * @return a list of actions for context menu
   */
  @Nonnull
  default List<? extends Action> getContextActions(@Nonnull PsiElement element) {
    return Collections.emptyList();
  }
}
