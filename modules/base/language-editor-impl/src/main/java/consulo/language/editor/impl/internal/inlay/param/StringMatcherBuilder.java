/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.impl.internal.inlay.param;

/**
 * Builder for StringMatcher instances based on wildcard ('*') patterns.
 */
public final class StringMatcherBuilder {
    private StringMatcherBuilder() {
        // Utility class
    }

    /**
     * Creates a StringMatcher for the given pattern, or returns null if the pattern is invalid.
     * Asterisks '*' are allowed as wildcards (max two), representing prefix, suffix, or contains matching.
     */
    public static StringMatcher create(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new StringMatcherImpl(s -> true);
        }
        return createAsterisksMatcher(pattern);
    }

    private static StringMatcher createAsterisksMatcher(String pattern) {
        long asterisksCount = pattern.chars().filter(ch -> ch == '*').count();
        if (asterisksCount > 2) {
            return null;
        }
        if (asterisksCount == 0) {
            return new StringMatcherImpl(s -> s.equals(pattern));
        }
        if ("*".equals(pattern)) {
            return new StringMatcherImpl(s -> true);
        }
        if (pattern.startsWith("*") && asterisksCount == 1) {
            String target = pattern.substring(1);
            return new StringMatcherImpl(s -> s.endsWith(target));
        }
        if (pattern.endsWith("*") && asterisksCount == 1) {
            String target = pattern.substring(0, pattern.length() - 1);
            return new StringMatcherImpl(s -> s.startsWith(target));
        }
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            String target = pattern.substring(1, pattern.length() - 1);
            return new StringMatcherImpl(s -> s.contains(target));
        }
        // More than one asterisk in non-supported positions is invalid
        return null;
    }
}
