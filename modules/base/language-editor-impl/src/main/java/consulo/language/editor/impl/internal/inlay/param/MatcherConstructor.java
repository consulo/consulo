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

import consulo.util.lang.Couple;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Constructs MethodMatcher instances from string patterns.
 */
public class MatcherConstructor {
    /**
     * Splits a matcher string into method name and params matcher.
     */
    public static Couple<String> extract(String matcher) {
        String trimmed = matcher.trim();
        if (trimmed.isEmpty()) return null;

        int openIndex = trimmed.lastIndexOf('(');
        if (openIndex < 0) {
            return Couple.of(trimmed, "");
        }
        else if (openIndex == 0) {
            String params = getParamsMatcher(trimmed);
            return params == null ? null : Couple.of("", params);
        }

        String methodPart = trimmed.substring(0, openIndex);
        String params = getParamsMatcher(trimmed);
        return (params == null)
            ? null
            : Couple.of(methodPart.trim(), params.trim());
    }

    private static String getParamsMatcher(String text) {
        int open = text.lastIndexOf('(');
        int close = text.lastIndexOf(')');
        if (open >= 0 && close > open) {
            return text.substring(open, close + 1).trim();
        }
        return null;
    }

    private static ParamMatcher createParametersMatcher(String paramsMatcher) {
        if (paramsMatcher.length() <= 2) return null;
        String inner = paramsMatcher.substring(1, paramsMatcher.length() - 1);
        List<String> parts = Arrays.stream(inner.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
        if (parts.stream().anyMatch(String::isEmpty)) return null;
        List<StringMatcher> matchers = parts.stream()
            .map(StringMatcherBuilder::create)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return (matchers.size() == parts.size()) ? new StringParamMatcher(matchers) : null;
    }

    /**
     * Creates a Matcher or returns null on parse failure.
     */
    public static Matcher createMatcher(String pattern) {
        Couple<String> pair = extract(pattern);
        if (pair == null) return null;
        StringMatcher methodMatcher = StringMatcherBuilder.create(pair.getFirst());
        if (methodMatcher == null) return null;
        ParamMatcher paramMatcher = pair.getSecond().isEmpty()
            ? AnyParamMatcher.INSTANCE
            : createParametersMatcher(pair.getSecond());
        return (paramMatcher != null) ? new Matcher(methodMatcher, paramMatcher) : null;
    }
}
