// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public interface UrlQueryParameter {
    String getName();

    @Nullable String getDescription();

    boolean getRepeatable();

    @Nullable PsiElement resolveToPsiElement();
}
