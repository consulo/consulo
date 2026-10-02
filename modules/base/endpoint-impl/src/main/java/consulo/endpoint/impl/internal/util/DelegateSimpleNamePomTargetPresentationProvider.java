// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public final class DelegateSimpleNamePomTargetPresentationProvider extends TypePresentationProvider<DelegateSimpleNamePomTarget> {
    @Override
    public Class<DelegateSimpleNamePomTarget> getItemClass() {
        return DelegateSimpleNamePomTarget.class;
    }

    @Override
    public @Nullable String getName(DelegateSimpleNamePomTarget t) {
        return t.getName();
    }

    @Override
    public @Nullable Image getIcon(DelegateSimpleNamePomTarget t) {
        return t.getIcon();
    }

    @Override
    public @Nullable String getTypeName(DelegateSimpleNamePomTarget t) {
        return t.getTypeName().getNullIfEmpty();
    }
}
