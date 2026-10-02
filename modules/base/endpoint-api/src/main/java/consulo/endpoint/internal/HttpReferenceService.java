// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.endpoint.url.parameter.PathVariableDefinitionsSearcher;
import consulo.endpoint.url.parameter.PathVariableUsagesProvider;
import consulo.endpoint.url.parameter.QueryParameterNameTarget;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.endpoint.url.reference.UrlPathReferenceTarget;
import consulo.endpoint.url.reference.UrlSegmentReference;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ResolveResult;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Separates implementation of URL / HTTP references resolve and navigation from API.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HttpReferenceService {
    static HttpReferenceService getInstance() {
        return Application.get().getInstance(HttpReferenceService.class);
    }

    PsiElement resolveMimeReference(PsiElement referenceElement, String text);

    boolean isReferenceToMimeElement(PsiElement element, String text);

    PsiElement resolveHeaderReference(PsiElement referenceElement, String text);

    boolean isReferenceToHeaderElement(PsiElement element, String text);

    PsiElement resolveHttpMethod(PsiElement referenceElement, String text, TextRange rangeInElement);

    boolean isReferenceToHttpMethod(PsiElement element, String text);

    default PsiElement resolveAuthorityReference(UrlSegmentReference reference, PsiElement referenceElement, String refValue) {
        return resolveAuthorityReference(reference, referenceElement, refValue, null);
    }

    PsiElement resolveAuthorityReference(
        UrlSegmentReference reference,
        PsiElement referenceElement,
        String refValue,
        @Nullable Consumer<UrlSegmentReference> customNavigate
    );

    UrlPathReferenceTarget createUrlPathTarget(UrlPathContext context, boolean isAtEnd, Project project);

    boolean isReferenceToUrlPathTarget(PsiElement element);

    NavigatablePsiElement createSearchableUrlElement(Project project, UrlPathContext context);

    @Nullable UrlPathReference getUrlFromPomTargetPsi(PsiElement psiElement);

    QueryParameterNameTarget createQueryParameterNameTarget(UrlPathContext context, String refValue, Project project);

    Object[] getQueryParameterNameVariants(@Nullable QueryParameterNameTarget target);

    PomTargetPsiElement resolvePathVariableDeclaration(
        String value,
        PsiElement referenceElement,
        TextRange range,
        PathVariableUsagesProvider usagesProvider
    );

    boolean canResolveToPathVariableDeclaration(Class<? extends PsiElement> elementClass);

    ResolveResult[] resolvePathVariableUsage(String variableName, PsiElement referenceElement, PathVariableDefinitionsSearcher searcher);

    boolean isReferenceToPathVariableDeclaration(PsiElement element);
}
