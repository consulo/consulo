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
package consulo.it.daemon;

import org.jspecify.annotations.Nullable;

/**
 * How a {@code descr} written in the markup is compared with the text a highlight or a line marker actually
 * carries: equal, either side a wildcard, or a shortened form ending in an ellipsis matching by prefix.
 *
 * @author VISTALL
 */
final class DescriptionMatch {
    private static final String ELLIPSIS = "...";

    private DescriptionMatch() {
    }

    static boolean matches(@Nullable String expected, @Nullable String actual) {
        if (ExpectedHighlight.ANY_TEXT.equals(expected) || ExpectedHighlight.ANY_TEXT.equals(actual)) {
            return true;
        }
        if (expected == null || actual == null) {
            return expected == null && actual == null;
        }
        if (expected.equals(actual)) {
            return true;
        }
        return isEllipsisPrefixOf(expected, actual) || isEllipsisPrefixOf(actual, expected);
    }

    private static boolean isEllipsisPrefixOf(String shortened, String full) {
        if (!shortened.endsWith(ELLIPSIS)) {
            return false;
        }
        return full.startsWith(shortened.substring(0, shortened.length() - ELLIPSIS.length()));
    }
}
