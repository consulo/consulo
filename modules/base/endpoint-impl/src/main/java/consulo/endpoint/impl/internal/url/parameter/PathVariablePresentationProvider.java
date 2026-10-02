// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.parameter.PathVariablePomTarget;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public final class PathVariablePresentationProvider extends TypePresentationProvider<PathVariablePomTarget> {
    @Override
    public Class<PathVariablePomTarget> getItemClass() {
        return PathVariablePomTarget.class;
    }

    @Override
    public @Nullable String getName(PathVariablePomTarget t) {
        return null;
    }

    @Override
    public String getTypeName(PathVariablePomTarget t) {
        return EndpointLocalize.microservicesUrlPathVariableTypeName().get();
    }

    @Override
    public Image getIcon(PathVariablePomTarget t) {
        return PlatformIconGroup.nodesVariable();
    }
}
