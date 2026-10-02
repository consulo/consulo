// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.endpoint.url.UrlTargetInfo;
import org.jspecify.annotations.Nullable;

public interface EndpointElementItem<G, E> extends EndpointListItem {
    @Nullable EndpointModuleEntity getModule();

    EndpointProvider<G, E> getProvider();

    G getGroup();

    E getEndpoint();

    boolean isValid();

    /**
     * Return {@link UrlTargetInfo}s for {@link #getGroup()} and {@link #getEndpoint()}
     * if {@link #getProvider()} is {@link EndpointUrlTargetProvider}
     */
    default @Nullable Iterable<UrlTargetInfo> getUrlTargetInfos() {
        return null;
    }
}
