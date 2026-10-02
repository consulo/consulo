// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;

/**
 * An interface that allows frameworks to provide additional info about PathVariable usage,
 * especially for cases when usages could not be located by generic Find Usages
 *
 * @see consulo.endpoint.url.parameter.PathVariableSem
 */
public interface PathVariableUsagesProvider extends SemDefinitionProvider {
    /**
     * Suggests names of already used Path Variables in the current context to help user declare a new Path Variable
     */
    Iterable<LookupElement> getCompletionVariantsForDeclaration(PsiElement context);
}
