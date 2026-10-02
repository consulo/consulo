// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import java.util.List;

public final class UrlConstants {
    public static final List<String> WS_SCHEMES = List.of("ws://", "wss://");

    public static final String HTTP_SCHEME = "http://";
    public static final String LOCALHOST = "localhost";

    public static final List<String> HTTP_SCHEMES = List.of("http://", "https://");

    public static final List<List<String>> KNOWN_SCHEMES_GROUPS = List.of(HTTP_SCHEMES, WS_SCHEMES);

    public static final List<String> HTTP_METHODS = List.of(
        HttpMethodConstants.GET,
        HttpMethodConstants.HEAD,
        HttpMethodConstants.POST,
        HttpMethodConstants.PUT,
        HttpMethodConstants.DELETE,
        HttpMethodConstants.CONNECT,
        HttpMethodConstants.PATCH,
        HttpMethodConstants.OPTIONS,
        HttpMethodConstants.TRACE
    );

    private UrlConstants() {
    }
}
