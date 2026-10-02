// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.parameter.QueryParameterNameReference;
import consulo.endpoint.url.parameter.QueryParameterSem;
import consulo.endpoint.url.parameter.RenameableSemElementSupport;
import consulo.endpoint.url.parameter.RenameableSemElementUtil;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.sem.SemKey;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class QueryParameterSemElementSupport implements RenameableSemElementSupport<QueryParameterSem> {
    public static final QueryParameterSemElementSupport INSTANCE = new QueryParameterSemElementSupport();

    private QueryParameterSemElementSupport() {
    }

    private static PomTargetPsiElement createQueryParameterInfoPomTargetElement(
        Project project,
        UrlPathContext context,
        String name,
        boolean forceFindUsages
    ) {
        QueryParameterNamePomTarget pomTarget =
            new QueryParameterNamePomTarget(new UrlPathReferenceUnifiedPomTarget(context, project), project, name);
        return new QueryParameterInfoFakeElement(project, pomTarget, forceFindUsages);
    }

    @Override
    public Iterable<PsiElement> findReferencingPsiElements(PomTarget pomTarget) {
        if (!(pomTarget instanceof QueryParameterNamePomTarget queryParameterNamePomTarget)) {
            return List.of();
        }
        @Nullable PsiElement paramNavigatable = queryParameterNamePomTarget.getParamNavigatable();
        if (paramNavigatable == null || RenameableSemElementUtil.getSemElement(this, paramNavigatable) == null) {
            return List.of();
        }
        return List.of(paramNavigatable);
    }

    @Override
    public boolean supportsTarget(PomTarget pomTarget) {
        return pomTarget instanceof QueryParameterNamePomTarget;
    }

    @Override
    @RequiredReadAction
    public PomTargetPsiElement createPomTargetPsi(Project project, QueryParameterSem sem) {
        return createQueryParameterInfoPomTargetElement(
            project,
            sem.getUrlPathContext(),
            sem.getName(),
            true
        );
    }

    @Override
    public SemKey<QueryParameterSem> getSemKey() {
        return QueryParameterSem.QUERY_PARAMETER_SEM_KEY;
    }

    @RequiredReadAction
    public static @Nullable PsiElement getNavigatablePsiElement(QueryParameterNameReference reference) {
        if (!(reference.resolve() instanceof QueryParameterInfoFakeElement element)) {
            return null;
        }
        if (!(element.getTarget() instanceof QueryParameterNamePomTarget target)) {
            return null;
        }
        return target.getNavigatablePsiElement();
    }
}
