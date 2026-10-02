// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.ResolvingHint;
import consulo.util.collection.ContainerUtil;

/**
 * Represents a reference to the element in code where the Path Variable is declared
 * EXAMPLE: in {@code "/users/{id}/delete"} it is {@code "{id}"}
 * <p>
 * in many cases it is interchangeable with {@link PathVariablePsiElement} and {@link PathVariablePomTarget}
 *
 * @see PathVariablePsiElement
 * @see PathVariablePomTarget
 */
public final class PathVariableDeclaringReference extends PsiReferenceBase<PsiElement> implements ResolvingHint {
    private final PathVariableUsagesProvider myVariableUsagesProvider;

    public PathVariableDeclaringReference(PsiElement host, TextRange range, PathVariableUsagesProvider variableUsagesProvider) {
        super(host, range, false);
        myVariableUsagesProvider = variableUsagesProvider;
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return ContainerUtil.collect(myVariableUsagesProvider.getCompletionVariantsForDeclaration(getElement()).iterator())
            .toArray(LookupElement.EMPTY_ARRAY);
    }

    @Override
    public String toString() {
        return "PathVariableDeclaringReference(" + getValue() + ", " + getRangeInElement() + ")";
    }

    @Override
    @RequiredReadAction
    public PomTargetPsiElement resolve() {
        HttpReferenceService service = Application.get().getInstance(HttpReferenceService.class);
        return service.resolvePathVariableDeclaration(getValue(), getElement(), getRangeInElement(), myVariableUsagesProvider);
    }

    @Override
    public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
        HttpReferenceService service = Application.get().getInstance(HttpReferenceService.class);
        return service.canResolveToPathVariableDeclaration(elementClass);
    }
}
