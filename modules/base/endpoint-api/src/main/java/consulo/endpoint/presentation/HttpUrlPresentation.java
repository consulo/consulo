// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.presentation;

import consulo.colorScheme.TextAttributesKey;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public final class HttpUrlPresentation extends PresentationData {
    public HttpUrlPresentation(@Nullable String httpUrl, @Nullable String definitionSource, @Nullable Image icon) {
        this(httpUrl, definitionSource, icon, null);
    }

    public HttpUrlPresentation(
        @Nullable String httpUrl,
        @Nullable String definitionSource,
        @Nullable Image icon,
        @Nullable TextAttributesKey attributesKey
    ) {
        super(httpUrl, definitionSource, icon, attributesKey);
    }
}
