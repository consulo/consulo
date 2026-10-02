// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.reference.UrlSegmentReferenceTarget;
import consulo.endpoint.util.CommonFakeNavigatablePomTarget;
import consulo.project.Project;

class QueryParameterInfoFakeElement extends CommonFakeNavigatablePomTarget implements UrlSegmentReferenceTarget {
    private final QueryParameterNamePomTarget myQueryParameterPomTarget;
    private final boolean myForceFindUsages;

    QueryParameterInfoFakeElement(Project project, QueryParameterNamePomTarget queryParameterPomTarget, boolean forceFindUsages) {
        super(project, queryParameterPomTarget);
        myQueryParameterPomTarget = queryParameterPomTarget;
        myForceFindUsages = forceFindUsages;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        if (myForceFindUsages) {
            showFindUsages();
            return;
        }
        if (myQueryParameterPomTarget.canNavigate()) {
            myQueryParameterPomTarget.navigate(requestFocus);
        }
        else {
            super.navigate(requestFocus);
        }
    }
}
