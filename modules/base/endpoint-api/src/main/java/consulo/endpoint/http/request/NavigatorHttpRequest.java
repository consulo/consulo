// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http.request;

import consulo.util.lang.Pair;

import java.util.List;

public final class NavigatorHttpRequest {
    private final String myUrl;
    private final String myRequestMethod;
    private final List<Pair<String, String>> myHeaders;
    private final List<Pair<String, String>> myParams;

    public NavigatorHttpRequest(
        String url,
        String requestMethod,
        List<Pair<String, String>> headers,
        List<Pair<String, String>> params
    ) {
        myUrl = url;
        myRequestMethod = requestMethod;
        myHeaders = headers;
        myParams = params;
    }

    public String getUrl() {
        return myUrl;
    }

    public String getRequestMethod() {
        return myRequestMethod;
    }

    public List<Pair<String, String>> getHeaders() {
        return myHeaders;
    }

    public List<Pair<String, String>> getParams() {
        return myParams;
    }
}
