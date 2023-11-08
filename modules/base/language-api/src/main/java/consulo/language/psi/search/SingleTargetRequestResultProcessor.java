// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceService;
import consulo.language.psi.ReferenceRange;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author peter
 */
public final class SingleTargetRequestResultProcessor extends RequestResultProcessor {
  private final PsiElement myTarget;
  @Nonnull
  private final PsiReferenceService myPsiReferenceService;

  public SingleTargetRequestResultProcessor(@Nonnull PsiElement target) {
    this(target, PsiReferenceService.getService());
  }

  public SingleTargetRequestResultProcessor(@Nonnull PsiElement target, @Nonnull PsiReferenceService psiReferenceService) {
    super(target);
    myTarget = target;
    myPsiReferenceService = psiReferenceService;
  }

  @Override
  @RequiredReadAction
  public boolean processTextOccurrence(@Nonnull PsiElement element, int offsetInElement, @Nonnull final Predicate<? super PsiReference> consumer) {
    if (!myTarget.isValid()) {
      return false;
    }

    final List<PsiReference> references = myPsiReferenceService.getReferences(element, new PsiReferenceService.Hints(myTarget, offsetInElement));
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < references.size(); i++) {
      PsiReference ref = references.get(i);
      ProgressManager.checkCanceled();
      if (ReferenceRange.containsOffsetInElement(ref, offsetInElement) && ref.isReferenceTo(myTarget) && !consumer.test(ref)) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("HardCodedStringLiteral")
  @Override
  public String toString() {
    return "SingleTarget: " + myTarget;
  }
}
