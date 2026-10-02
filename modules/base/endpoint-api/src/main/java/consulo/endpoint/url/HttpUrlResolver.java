// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class HttpUrlResolver implements UrlResolver {
    public static final List<Authority.Exact> HTTP_AUTHORITY =
        List.of(new Authority.Exact("localhost:8080"), new Authority.Exact("localhost"));

    private final List<String> mySupportedSchemes = UrlConstants.HTTP_SCHEMES;

    @Override
    public List<Authority.Exact> getAuthorityHints(@Nullable String schema) {
        return HTTP_AUTHORITY;
    }

    @Override
    public List<String> getSupportedSchemes() {
        return mySupportedSchemes;
    }
}
