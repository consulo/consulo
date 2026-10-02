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
package consulo.sandboxPlugin.ide.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.url.UrlConstants;
import consulo.language.psi.ElementManipulators;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import org.jspecify.annotations.Nullable;

public final class SandEndpointLiteral {
    private static final String CLIENT_PREFIX = "CALL ";
    private static final String DEPRECATED_PREFIX = "!";

    private final boolean myClient;
    private final boolean myDeprecated;
    private final String myMethod;
    private final String myUrl;
    private final int myUrlOffset;

    private SandEndpointLiteral(boolean client, boolean deprecated, String method, String url, int urlOffset) {
        myClient = client;
        myDeprecated = deprecated;
        myMethod = method;
        myUrl = url;
        myUrlOffset = urlOffset;
    }

    public boolean isClient() {
        return myClient;
    }

    public boolean isDeprecated() {
        return myDeprecated;
    }

    public String getMethod() {
        return myMethod;
    }

    public String getUrl() {
        return myUrl;
    }

    public int getUrlOffset() {
        return myUrlOffset;
    }

    public boolean isAbsoluteUrl() {
        return myUrl.startsWith("http://") || myUrl.startsWith("https://");
    }

    @RequiredReadAction
    public static @Nullable SandEndpointLiteral of(SandStringExpression expression) {
        return parse(getValue(expression));
    }

    @RequiredReadAction
    public static String getValue(SandStringExpression expression) {
        return ElementManipulators.getValueText(expression);
    }

    @RequiredReadAction
    public static TextRange getValueRange(SandStringExpression expression) {
        return ElementManipulators.getValueTextRange(expression);
    }

    public static boolean isUrlLike(String text) {
        return text.startsWith("/") || text.startsWith("http://") || text.startsWith("https://");
    }

    public static @Nullable SandEndpointLiteral parse(String value) {
        boolean client = value.startsWith(CLIENT_PREFIX);
        boolean deprecated = !client && value.startsWith(DEPRECATED_PREFIX);
        int methodStart = client ? CLIENT_PREFIX.length() : deprecated ? DEPRECATED_PREFIX.length() : 0;

        int space = value.indexOf(' ', methodStart);
        if (space < 0) {
            return null;
        }

        String method = value.substring(methodStart, space);
        if (!UrlConstants.HTTP_METHODS.contains(method)) {
            return null;
        }

        String url = value.substring(space + 1);
        if (client ? !isUrlLike(url) : !url.startsWith("/")) {
            return null;
        }
        return new SandEndpointLiteral(client, deprecated, method, url, space + 1);
    }
}
