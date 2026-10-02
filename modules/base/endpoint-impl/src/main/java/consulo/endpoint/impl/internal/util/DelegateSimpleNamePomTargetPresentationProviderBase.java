// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.application.presentation.TypePresentationProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

abstract class DelegateSimpleNamePomTargetPresentationProviderBase<T extends DelegateSimpleNamePomTarget>
    extends TypePresentationProvider<T> {
    @Override
    public @Nullable String getName(T t) {
        return t.getName();
    }

    @Override
    public @Nullable Image getIcon(T t) {
        return t.getIcon();
    }

    @Override
    public @Nullable String getTypeName(T t) {
        return t.getTypeName().getNullIfEmpty();
    }
}
