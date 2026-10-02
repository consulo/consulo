// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface UrlResolver {
    /**
     * @param schema schema string with {@code ://} in the end, like {@code http://} or {@code wss://}.
     * If not specified, then all authorities should be returned
     */
    default List<Authority.Exact> getAuthorityHints(@Nullable String schema) {
        return List.of();
    }

    List<String> getSupportedSchemes();

    Iterable<UrlTargetInfo> resolve(UrlResolveRequest request);

    Iterable<UrlTargetInfo> getVariants();
}
