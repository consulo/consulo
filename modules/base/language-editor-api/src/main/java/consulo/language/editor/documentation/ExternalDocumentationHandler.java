// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.documentation;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implement additionally in your {@link DocumentationProvider}.
 */
public interface ExternalDocumentationHandler {
  boolean handleExternal(PsiElement element, PsiElement originalElement);

  boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context);

  boolean canFetchDocumentationLink(String link);

  @Nonnull
  String fetchExternalDocumentation(@Nonnull String link, @Nullable PsiElement element);

  /**
   * Defines whether we will show external documentation
   * link at the bottom of the documentation pane or not.
   *
   * @return true if external documentation link should be
   * shown, false otherwise
   */
  default boolean canHandleExternal(@Nullable PsiElement element, @Nullable PsiElement originalElement) {
    return true;
  }

  /**
   * This method can supply a target (HTML reference), which will be navigated to on showing of
   * {@link #fetchExternalDocumentation(String, PsiElement)}) result.
   *
   * @see DocumentationManagerProtocol
   */
  @Nullable
  default String extractRefFromLink(@Nonnull String link) {
    return null;
  }
}
