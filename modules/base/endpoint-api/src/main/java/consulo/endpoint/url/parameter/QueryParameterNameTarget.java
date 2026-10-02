// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.pom.PomRenameableTarget;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public interface QueryParameterNameTarget extends PomRenameableTarget<@Nullable Object> {
    PsiElement toElement(boolean forceFindUsagesOnNavigate);
}
