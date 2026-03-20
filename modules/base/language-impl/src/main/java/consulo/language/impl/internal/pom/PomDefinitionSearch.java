// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.pom;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.search.DefinitionsScopedSearchExecutor;

import java.util.function.Predicate;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class PomDefinitionSearch implements DefinitionsScopedSearchExecutor {
    @Override
    public boolean execute(
        DefinitionsScopedSearch.SearchParameters queryParameters,
        Predicate<? super PsiElement> consumer
    ) {
        return !(queryParameters.getElement() instanceof PomTargetPsiElement targetPsiElement
            && targetPsiElement.getTarget() instanceof PsiTarget target
            && !consumer.test(target.getNavigationElement()));
    }
}
