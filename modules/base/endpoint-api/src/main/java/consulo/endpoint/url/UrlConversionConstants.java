// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import java.util.regex.Pattern;

public final class UrlConversionConstants {
    public static final Pattern STANDARD_PATH_VARIABLE_NAME_PATTERN = Pattern.compile("[\\w-]+", Pattern.UNICODE_CHARACTER_CLASS);

    public static final Pattern SPRING_LIKE_PATH_VARIABLE_PATTERN =
        Pattern.compile("([\\w-]+)\\s*:?(.+)?", Pattern.UNICODE_CHARACTER_CLASS);

    public static final UrlSpecialSegmentMarker SPRING_LIKE_PATH_VARIABLE_BRACES =
        new UrlSpecialSegmentMarker("{", "}", SPRING_LIKE_PATH_VARIABLE_PATTERN);

    public static final UrlSpecialSegmentMarker SPRING_LIKE_PLACEHOLDER_BRACES = new UrlSpecialSegmentMarker("${", "}");

    private UrlConversionConstants() {
    }
}
