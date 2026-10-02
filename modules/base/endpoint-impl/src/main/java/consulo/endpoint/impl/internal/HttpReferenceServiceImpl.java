// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.document.util.TextRange;
import consulo.endpoint.impl.internal.http.HttpHeaderElement;
import consulo.endpoint.impl.internal.http.HttpMethodElement;
import consulo.endpoint.impl.internal.mime.MimeTypePsiElement;
import consulo.endpoint.impl.internal.url.reference.AuthorityPomTarget;
import consulo.endpoint.impl.internal.url.reference.AuthorityReferenceFakeElement;
import consulo.endpoint.impl.internal.url.reference.QueryParameterNamePomTarget;
import consulo.endpoint.impl.internal.url.reference.UrlPathReferenceUnifiedPomTarget;
import consulo.endpoint.impl.internal.url.reference.UrlTargetInfoFakeElement;
import consulo.endpoint.internal.HttpReferenceService;
import consulo.endpoint.url.HttpMethodConstants;
import consulo.endpoint.url.UrlQueryParameter;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.parameter.PathVariableDefinitionsSearcher;
import consulo.endpoint.url.parameter.PathVariablePsiElement;
import consulo.endpoint.url.parameter.PathVariableUsagesProvider;
import consulo.endpoint.url.parameter.QueryParameterNameTarget;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.endpoint.url.reference.UrlPathReferenceTarget;
import consulo.endpoint.url.reference.UrlSegmentReference;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ServiceImpl
@Singleton
public final class HttpReferenceServiceImpl implements HttpReferenceService {
    @Override
    public PsiElement resolveMimeReference(PsiElement referenceElement, String text) {
        return new MimeTypePsiElement(referenceElement, text);
    }

    @Override
    public boolean isReferenceToMimeElement(PsiElement element, String text) {
        return element instanceof MimeTypePsiElement mimeTypePsiElement && text.equals(mimeTypePsiElement.getName());
    }

    @Override
    public PsiElement resolveHeaderReference(PsiElement referenceElement, String text) {
        return new HttpHeaderElement(referenceElement, text);
    }

    @Override
    public boolean isReferenceToHeaderElement(PsiElement element, String text) {
        return element instanceof HttpHeaderElement httpHeaderElement && text.equals(httpHeaderElement.getName());
    }

    @Override
    public PsiElement resolveHttpMethod(PsiElement referenceElement, String text, TextRange rangeInElement) {
        return new HttpMethodElement(referenceElement, text, rangeInElement);
    }

    @Override
    public boolean isReferenceToHttpMethod(PsiElement element, String text) {
        return element instanceof HttpMethodElement httpMethodElement && text.equals(httpMethodElement.getName());
    }

    @Override
    public PsiElement resolveAuthorityReference(
        UrlSegmentReference reference,
        PsiElement referenceElement,
        String refValue,
        @Nullable Consumer<UrlSegmentReference> customNavigate
    ) {
        return new AuthorityReferenceFakeElement(
            referenceElement.getProject(),
            new AuthorityPomTarget(refValue),
            customNavigate != null ? () -> customNavigate.accept(reference) : null
        );
    }

    @Override
    @RequiredReadAction
    public UrlPathReferenceTarget createUrlPathTarget(UrlPathContext context, boolean isAtEnd, Project project) {
        return new UrlPathReferenceUnifiedPomTarget(context, isAtEnd, project);
    }

    @Override
    public boolean isReferenceToUrlPathTarget(PsiElement element) {
        return element instanceof UrlTargetInfoFakeElement;
    }

    @Override
    @RequiredReadAction
    public NavigatablePsiElement createSearchableUrlElement(Project project, UrlPathContext context) {
        UrlPathReferenceUnifiedPomTarget pomTarget = new UrlPathReferenceUnifiedPomTarget(context, project);
        return new UrlTargetInfoFakeElement(project, pomTarget, null, context.isDeclaration());
    }

    @Override
    @RequiredReadAction
    public @Nullable UrlPathReference getUrlFromPomTargetPsi(PsiElement psiElement) {
        return psiElement instanceof UrlTargetInfoFakeElement fakeElement ? fakeElement.getReference() : null;
    }

    @Override
    @RequiredReadAction
    public QueryParameterNameTarget createQueryParameterNameTarget(UrlPathContext context, String refValue, Project project) {
        return new QueryParameterNamePomTarget(new UrlPathReferenceUnifiedPomTarget(context, project), project, refValue);
    }

    @Override
    public Object[] getQueryParameterNameVariants(@Nullable QueryParameterNameTarget target) {
        if (!(target instanceof QueryParameterNamePomTarget queryParameterNamePomTarget)) {
            return new Object[0];
        }
        List<Object> result = new ArrayList<>();
        for (UrlTargetInfo info : queryParameterNamePomTarget.getUrlPathReferenceUnifiedPomTarget().getResolvedTargets()) {
            if (!(info.getMethods().isEmpty() || info.getMethods().contains(HttpMethodConstants.GET))) {
                continue;
            }
            for (UrlQueryParameter param : info.getQueryParameters()) {
                result.add(LookupElementBuilder.create(param.getName()).withIcon(PlatformIconGroup.nodesParameter()));
            }
        }
        return result.toArray();
    }

    @Override
    public PomTargetPsiElement resolvePathVariableDeclaration(
        String value,
        PsiElement referenceElement,
        TextRange range,
        PathVariableUsagesProvider usagesProvider
    ) {
        return PathVariablePsiElement.create(value, referenceElement, range, usagesProvider);
    }

    @Override
    public boolean canResolveToPathVariableDeclaration(Class<? extends PsiElement> elementClass) {
        return ReflectionUtil.isAssignable(PathVariablePsiElement.class, elementClass);
    }

    @Override
    public ResolveResult[] resolvePathVariableUsage(
        String variableName,
        PsiElement referenceElement,
        PathVariableDefinitionsSearcher searcher
    ) {
        @Nullable PathVariablePsiElement merge = PathVariablePsiElement.merge(
            searcher.getPathVariables(referenceElement)
                .map(it -> (PathVariablePsiElement) it)
                .filter(o -> variableName.equals(o.getName()))
                .map(PathVariablePsiElement::navigatingToDeclaration)
                .toList()
        );
        if (merge == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return PsiElementResolveResult.createResults(merge);
    }

    @Override
    public boolean isReferenceToPathVariableDeclaration(PsiElement element) {
        return element instanceof PathVariablePsiElement;
    }
}
