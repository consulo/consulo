// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.UrlQueryParameter;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.parameter.QueryParameterNameTarget;
import consulo.endpoint.util.SimpleNamePomTarget;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

public class QueryParameterNamePomTarget extends SimpleNamePomTarget implements QueryParameterNameTarget {
    private final UrlPathReferenceUnifiedPomTarget myUrlPathReferenceUnifiedPomTarget;
    private final Project myProject;

    public QueryParameterNamePomTarget(UrlPathReferenceUnifiedPomTarget urlPathReferenceUnifiedPomTarget, Project project, String name) {
        super(name);
        myUrlPathReferenceUnifiedPomTarget = urlPathReferenceUnifiedPomTarget;
        myProject = project;
    }

    public UrlPathReferenceUnifiedPomTarget getUrlPathReferenceUnifiedPomTarget() {
        return myUrlPathReferenceUnifiedPomTarget;
    }

    @RequiredReadAction
    public @Nullable PsiElement getNavigatablePsiElement() {
        @Nullable PsiElement paramNavigatable = getParamNavigatable();
        return paramNavigatable != null ? paramNavigatable : myUrlPathReferenceUnifiedPomTarget.getNavigatablePsiElement();
    }

    public @Nullable PsiElement getParamNavigatable() {
        for (UrlTargetInfo target : myUrlPathReferenceUnifiedPomTarget.getResolvedTargets()) {
            for (UrlQueryParameter parameter : target.getQueryParameters()) {
                if (!parameter.getName().equals(getName())) {
                    continue;
                }
                @Nullable PsiElement element = parameter.resolveToPsiElement();
                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        @Nullable PsiElement psiElement = getNavigatablePsiElement();

        if (psiElement instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(requestFocus);
        }
        else {
            myUrlPathReferenceUnifiedPomTarget.navigate(requestFocus);
        }
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return myUrlPathReferenceUnifiedPomTarget.canNavigate();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null || !super.equals(other)) {
            return false;
        }
        QueryParameterNamePomTarget that = (QueryParameterNamePomTarget) other;
        return myUrlPathReferenceUnifiedPomTarget.equals(that.myUrlPathReferenceUnifiedPomTarget);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + myUrlPathReferenceUnifiedPomTarget.hashCode();
        return result;
    }

    @Override
    public PsiElement toElement(boolean forceFindUsagesOnNavigate) {
        return new QueryParameterInfoFakeElement(myProject, this, forceFindUsagesOnNavigate);
    }
}
