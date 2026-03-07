// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.util.io.URLUtil;
import jakarta.annotation.Nonnull;

import java.net.URI;

public final class URIUtil {
    private URIUtil() {
    }

    public static @Nonnull String normalizeAndValidateHttpUri(@Nonnull String uri) {
        String normalized = URLUtil.addSchemaIfMissing(uri);
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith(URLUtil.HTTP_PROTOCOL)) {
            throw new IllegalArgumentException(CollaborationToolsLocalize.loginServerInvalid().get());
        }
        URI.create(normalized);
        return normalized;
    }

    public static boolean isValidHttpUri(@Nonnull String uri) {
        if (uri.isBlank()) {
            return false;
        }
        try {
            normalizeAndValidateHttpUri(uri);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean equalWithoutSchema(@Nonnull URI first, @Nonnull URI second) {
        String stubScheme = "stub";
        return UriUtilKt.withScheme(first, stubScheme).equals(UriUtilKt.withScheme(second, stubScheme));
    }

    public static @Nonnull String toStringWithoutScheme(@Nonnull URI uri) {
        String schemeText = uri.getScheme() + URLUtil.SCHEME_SEPARATOR;
        String uriString = uri.toString();
        if (uriString.startsWith(schemeText)) {
            return uriString.substring(schemeText.length());
        }
        return uriString;
    }

    public static @Nonnull URI createUriWithCustomScheme(@Nonnull String uri, @Nonnull String scheme) {
        String prefix = scheme + URLUtil.SCHEME_SEPARATOR;
        if (uri.startsWith(prefix)) {
            return URI.create(uri);
        }
        return URI.create(prefix + removeProtocolPrefix(uri));
    }

    private static @Nonnull String removeProtocolPrefix(@Nonnull String url) {
        int index = url.indexOf(URLUtil.SCHEME_SEPARATOR);
        return index != -1 ? url.substring(index + URLUtil.SCHEME_SEPARATOR.length()) : url;
    }

    public static @Nonnull URI resolveRelative(@Nonnull URI base, @Nonnull String path) {
        String newPath;
        if (path.startsWith("/")) {
            newPath = path.replaceAll("//+", "/");
        }
        else {
            String currentPath = base.toString(); // to avoid path decoding
            if (currentPath.endsWith("/")) {
                newPath = currentPath + path.replaceAll("//+", "/");
            }
            else {
                newPath = currentPath + "/" + path.replaceAll("//+", "/");
            }
        }

        return base.resolve(newPath).normalize();
    }

    public static @Nonnull URI withQuery(@Nonnull URI uri, @Nonnull String searchQuery) {
        if (searchQuery.isBlank()) {
            return uri;
        }
        String rawUri = uri.toString(); // to avoid path decoding
        return URI.create(rawUri + "?" + searchQuery);
    }
}
