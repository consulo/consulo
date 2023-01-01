// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.pom;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.search.DefinitionsScopedSearchExecutor;

import javax.annotation.Nonnull;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class PomDefinitionSearch implements DefinitionsScopedSearchExecutor {
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
