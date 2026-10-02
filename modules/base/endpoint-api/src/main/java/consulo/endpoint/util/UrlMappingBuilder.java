// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

public class UrlMappingBuilder {
    private final StringBuilder myBuilder = new StringBuilder();

    public UrlMappingBuilder() {
    }

    public UrlMappingBuilder(@Nullable String baseUrl) {
        if (baseUrl != null) {
            myBuilder.append(baseUrl);
        }
    }

    public UrlMappingBuilder appendSegment(@Nullable String part) {
        if (part == null) {
            return this;
        }

        if (myBuilder.length() > 0 && !StringUtil.endsWith(myBuilder, "/") && !part.startsWith("/")) {
            myBuilder.append('/');
        }
        if (StringUtil.endsWith(myBuilder, "/") && part.startsWith("/")) {
            myBuilder.append(part, 1, part.length());
        }
        else {
            myBuilder.append(part);
        }
        return this;
    }

    public @Nullable String buildOrNull() {
        if (myBuilder.length() == 0) {
            return null;
        }
        return myBuilder.toString();
    }

    public String build() {
        return myBuilder.toString();
    }
}
