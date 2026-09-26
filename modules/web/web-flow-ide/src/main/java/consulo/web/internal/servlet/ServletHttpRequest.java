/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.internal.servlet;

import consulo.builtinWebServer.http.HttpRequest;
import consulo.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
final class ServletHttpRequest implements HttpRequest {
    private static final String REFERER_HEADER_NAME = "Referer";

    private final HttpServletRequest myRequest;
    private final HttpMethod myMethod;
    private final String myRawPrefix;
    private final String myPath;
    private final String myUri;

    private byte @Nullable [] myContent;
    private boolean myTerminated;

    ServletHttpRequest(HttpServletRequest request, HttpMethod method, String rawPrefix, String decodedRest, String rawRestWithQuery) {
        myRequest = request;
        myMethod = method;
        myRawPrefix = rawPrefix;
        myPath = decodedRest;
        myUri = rawRestWithQuery;
    }

    @Override
    public HttpMethod method() {
        return myMethod;
    }

    @Override
    public String getContentAsString(Charset charset) throws IOException {
        return new String(getContent(), charset);
    }

    @Override
    public byte[] getContent() throws IOException {
        byte[] content = myContent;
        if (content == null) {
            content = myRequest.getInputStream().readAllBytes();
            myContent = content;
        }
        return content;
    }

    @Override
    public String uri() {
        return myUri;
    }

    @Override
    public String path() {
        return myPath;
    }

    @Override
    public @Nullable String getHeaderValue(String headerName) {
        String value = myRequest.getHeader(headerName);
        if (value != null && REFERER_HEADER_NAME.equalsIgnoreCase(headerName)) {
            return stripPrefix(value);
        }
        return value;
    }

    @Override
    public @Nullable String getParameterValue(String parameter) {
        return myRequest.getParameter(parameter);
    }

    @Override
    public int localPort() {
        return myRequest.getLocalPort();
    }

    @Override
    public String contextPath() {
        return myRawPrefix;
    }

    @Override
    public void terminate() {
        myTerminated = true;
    }

    boolean isTerminated() {
        return myTerminated;
    }

    private String stripPrefix(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }

        int pathStart = url.indexOf('/', schemeEnd + 3);
        if (pathStart < 0 || !url.startsWith(myRawPrefix + "/", pathStart)) {
            return url;
        }
        return url.substring(0, pathStart) + url.substring(pathStart + myRawPrefix.length());
    }
}
