// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.endpoint.url.reference.UrlSegmentReference;
import consulo.endpoint.url.reference.UrlSegmentReferenceTarget;
import consulo.endpoint.util.CommonFakeNavigatablePomTarget;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class UrlTargetInfoFakeElement extends CommonFakeNavigatablePomTarget implements UrlSegmentReferenceTarget {
    private final UrlPathReferenceUnifiedPomTarget myUnifiedPomTarget;
    private final @Nullable UrlPathReference myReference;
    private final boolean myForceFindUsages;

    public UrlTargetInfoFakeElement(
        Project project,
        UrlPathReferenceUnifiedPomTarget unifiedPomTarget,
        @Nullable UrlPathReference reference,
        boolean forceFindUsages
    ) {
        super(project, unifiedPomTarget);
        myUnifiedPomTarget = unifiedPomTarget;
        myReference = reference;
        myForceFindUsages = forceFindUsages;
    }

    @Override
    @RequiredReadAction
    public @Nullable UrlPathReference getReference() {
        return myReference;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return (myReference != null && myReference.getCustomNavigate() != null) || super.canNavigateToSource();
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        @Nullable Consumer<UrlSegmentReference> customNavigate = myReference != null ? myReference.getCustomNavigate() : null;
        if (customNavigate != null) {
            customNavigate.accept(myReference);
            return;
        }

        if (myForceFindUsages) {
            showFindUsages();
            return;
        }

        if (myUnifiedPomTarget.canNavigate()) {
            myUnifiedPomTarget.navigate(requestFocus);
        }
        else {
            super.navigate(requestFocus);
        }
    }
}
