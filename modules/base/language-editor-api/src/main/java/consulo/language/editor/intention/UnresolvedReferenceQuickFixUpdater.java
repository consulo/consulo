// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.intention;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * Call each registered {@link UnresolvedReferenceQuickFixProvider} for its quick fixes.
 * It does roughly the same as {@link UnresolvedReferenceQuickFixProvider#registerReferenceFixes(PsiReference, QuickFixActionRegistrar)}, except:
 * 1) each {@link UnresolvedReferenceQuickFixProvider#registerFixes(PsiReference, QuickFixActionRegistrar)} is called in the background and
 * 2) in the lazy manner (no more than two providers in parallel).
 * That way we make highlighting to complete faster (because fewer {@link UnresolvedReferenceQuickFixProvider}s are called)
 * and avoid freezes (because dozens of providers are not run at the same time, taking resources unnecessarily)
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface UnresolvedReferenceQuickFixUpdater {
  static UnresolvedReferenceQuickFixUpdater getInstance(Project project) {
    return project.getInstance(UnresolvedReferenceQuickFixUpdater.class);
  }

  /**
   * Tell highlighting subsystem that this {@code info} was generated to highlight unresolved reference {@code ref}.
   * This call triggers background calculation of quick fixes supplied by {@link UnresolvedReferenceQuickFixProvider}
   * You can only call it from the highlighting (e.g. your {@link com.intellij.codeInsight.daemon.impl.HighlightVisitor})
   */
  void registerQuickFixesLater(@Nonnull PsiReference ref, @Nonnull HighlightInfo.Builder info);
}
