// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.client;

import consulo.http.localize.HttpLocalize;
import consulo.util.io.URLUtil;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

public final class URIUtil {
    private URIUtil() {
    }

    public static String normalizeAndValidateHttpUri(String uri) {
        String normalized = URLUtil.addSchemaIfMissing(uri);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith(URLUtil.HTTP_PROTOCOL)) {
            throw new IllegalArgumentException(HttpLocalize.loginServerInvalid().get());
        }
        URI.create(normalized);
        return normalized;
    }

    public static boolean isValidHttpUri(String uri) {
        if (uri.isBlank()) {
            return false;
        }
        try {
            normalizeAndValidateHttpUri(uri);
            return true;
        }
        catch (Throwable e) {
            return false;
        }
    }

    public static boolean equalWithoutSchema(URI first, URI second) {
        String stubScheme = "stub";
        return withScheme(first, stubScheme).equals(withScheme(second, stubScheme));
    }

    public static String toStringWithoutScheme(URI uri) {
        String schemeText = uri.getScheme() + URLUtil.SCHEME_SEPARATOR;
        String text = uri.toString();
        return text.startsWith(schemeText) ? text.substring(schemeText.length()) : text;
    }

    public static URI createUriWithCustomScheme(String uri, String scheme) {
        String prefix = scheme + URLUtil.SCHEME_SEPARATOR;
        if (uri.startsWith(prefix)) {
            return URI.create(uri);
        }
        return URI.create(prefix + removeProtocolPrefix(uri));
    }

    public static URI resolveRelative(URI uri, String path) {
        String newPath;
        if (path.startsWith("/")) {
            newPath = path.replace("//+", "/");
        }
        else {
            String currentPath = uri.toString();
            if (currentPath.endsWith("/")) {
                newPath = currentPath + path.replace("//+", "/");
            }
            else {
                newPath = currentPath + "/" + path.replace("//+", "/");
            }
        }
        return uri.resolve(newPath).normalize();
    }

    public static URI withQuery(URI uri, String searchQuery) {
        if (searchQuery.isBlank()) {
            return uri;
        }
        return URI.create(uri + "?" + searchQuery);
    }

    public static URI withFragment(URI uri, @Nullable String newFragment) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), newFragment);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static URI withScheme(URI uri, String scheme) {
        try {
            return new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String removeProtocolPrefix(String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        return index != -1 ? url.substring(index + URLUtil.SCHEME_SEPARATOR.length()) : url;
    }
}
