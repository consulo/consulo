// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import org.jspecify.annotations.Nullable;

@ExtensionAPI(ComponentScope.PROJECT)
public interface UrlResolverFactory {
    @Nullable UrlResolver forProject();
}
