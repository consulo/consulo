// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.pom;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;

import javax.annotation.Nonnull;

/**
 * @author Gregory.Shrago
 */
public class PomDefinitionSearch implements QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
  @Override
  public boolean execute(@Nonnull DefinitionsScopedSearch.SearchParameters queryParameters, @Nonnull Processor<? super PsiElement> consumer) {
    PsiElement queryParametersElement = queryParameters.getElement();
    if (queryParametersElement instanceof PomTargetPsiElement) {
      final PomTarget target = ((PomTargetPsiElement)queryParametersElement).getTarget();
      if (target instanceof PsiTarget) {
        if (!consumer.process(((PsiTarget)target).getNavigationElement())) return false;
      }
    }
    return true;
  }
}
