// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.sem.SemKey;
import org.jspecify.annotations.Nullable;

/**
 * A {@link consulo.language.sem.SemElement} that should be registered for {@link consulo.language.psi.PsiElement}s which are considered
 * as usages of {@link #getPathVariablePsiElement()}, in case when it couldn't be done by {@link consulo.language.psi.PsiReference}
 */
public interface PathVariableSem extends RenameableSemElement {
    SemKey<PathVariableSem> PATH_VARIABLE_SEM_KEY = SemKey.createKey("PathVariable", RenameableSemElement.RENAMEABLE_SEM_KEY);

    @Override
    String getName();

    /**
     * The path variable that this {@link PathVariableSem} resolves to.
     * {@code null} means that this element is not a PathVariable usage
     */
    @Nullable PathVariablePsiElement getPathVariablePsiElement();

    /**
     * @return {@code true} if the {@link consulo.language.psi.PsiElement}, that current {@link PathVariableSem} is registered for,
     * is an actual name holder, and it's name is reference for the Path Variable and should follow the renaming of the Path Variable
     * {@code false} if this {@link PathVariableSem} doesn't hold the name  for instance if there is an explicit reference
     * e.g. {@code @PathVariable("explicitName")})
     * if this property is {@code false} it means that current {@link PathVariableSem} is useless and should not participate in
     * reference operations (rename, find-usages)
     */
    boolean isActualNameHolder();
}
