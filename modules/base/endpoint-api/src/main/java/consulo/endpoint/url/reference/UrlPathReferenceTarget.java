// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.pom.PomRenameableTarget;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public interface UrlPathReferenceTarget extends PomRenameableTarget<@Nullable Object> {
    UrlPathContext getContext();

    @Nullable NavigatablePsiElement getNavigatablePsiElement();

    Set<UrlTargetInfo> getResolvedTargets();

    PsiElement toElement(UrlPathReference reference);
}
