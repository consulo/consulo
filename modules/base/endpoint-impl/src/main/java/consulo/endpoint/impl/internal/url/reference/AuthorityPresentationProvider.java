// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.reference;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public final class AuthorityPresentationProvider extends TypePresentationProvider<AuthorityPomTarget> {
    @Override
    public Class<AuthorityPomTarget> getItemClass() {
        return AuthorityPomTarget.class;
    }

    @Override
    public @Nullable String getName(AuthorityPomTarget t) {
        return null;
    }

    @Override
    public String getTypeName(AuthorityPomTarget t) {
        return EndpointLocalize.microservicesUrlPathAuthority().get();
    }

    @Override
    public Image getIcon(AuthorityPomTarget t) {
        return PlatformIconGroup.javaeeWebservice();
    }
}
