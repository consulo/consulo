// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.language.editor.highlight.HighlightedReference;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.ResolvingHint;

public class PathVariableUsageReference extends PsiPolyVariantReferenceBase<PsiElement> implements ResolvingHint, HighlightedReference {
    private final PathVariableDefinitionsSearcher mySearcher;

    public PathVariableUsageReference(PsiElement host, PathVariableDefinitionsSearcher searcher) {
        super(host, false);
        mySearcher = searcher;
    }

    public PathVariableUsageReference(PsiElement host, TextRange rangeInElement, PathVariableDefinitionsSearcher searcher) {
        super(host, rangeInElement);
        mySearcher = searcher;
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return mySearcher.getPathVariables(getElement()).toArray(new PomTargetPsiElement[0]);
    }

    @Override
    @RequiredReadAction
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        HttpReferenceService service = Application.get().getInstance(HttpReferenceService.class);
        return service.resolvePathVariableUsage(getValue(), getElement(), mySearcher);
    }

    @Override
    @RequiredReadAction
    public boolean isReferenceTo(PsiElement element) {
        HttpReferenceService service = Application.get().getInstance(HttpReferenceService.class);
        return service.isReferenceToPathVariableDeclaration(element) && super.isReferenceTo(element);
    }

    @Override
    public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
        HttpReferenceService service = Application.get().getInstance(HttpReferenceService.class);
        return service.canResolveToPathVariableDeclaration(elementClass);
    }
}
