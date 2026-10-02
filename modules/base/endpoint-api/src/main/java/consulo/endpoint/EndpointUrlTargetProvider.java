// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.endpoint.oas.OpenApiSpecification;
import consulo.endpoint.url.UrlTargetInfo;
import org.jspecify.annotations.Nullable;

public interface EndpointUrlTargetProvider<G, E> extends EndpointProvider<G, E> {
    Iterable<UrlTargetInfo> getUrlTargetInfo(G group, E endpoint);

    default @Nullable OpenApiSpecification getOpenApiSpecification(G group, E endpoint) {
        return null;
    }

    default boolean shouldShowOpenApiPanel() {
        return true;
    }
}
