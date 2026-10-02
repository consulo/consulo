// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.reference.UrlSegmentReferenceTarget;
import consulo.endpoint.util.CommonFakeNavigatablePomTarget;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

public class AuthorityReferenceFakeElement extends CommonFakeNavigatablePomTarget implements UrlSegmentReferenceTarget {
    private final @Nullable Runnable myCustomNavigate;

    public AuthorityReferenceFakeElement(Project project, AuthorityPomTarget unifiedPomTarget, @Nullable Runnable customNavigate) {
        super(project, unifiedPomTarget);
        myCustomNavigate = customNavigate;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return myCustomNavigate != null || super.canNavigateToSource();
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        if (myCustomNavigate != null) {
            myCustomNavigate.run();
            return;
        }

        super.navigate(requestFocus);
    }
}
