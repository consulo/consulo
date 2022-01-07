/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.hints.filtering;

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public class MatcherConstructor {
  @Nullable
  public static Couple<String> extract(String matcher) {
    String trimmedMatcher = matcher.trim();
    if (trimmedMatcher.isEmpty()) return null;

    int index = trimmedMatcher.indexOf('(');
    if (index < 0) {
      return Couple.of(trimmedMatcher, "");
    }
    else if (index == 0) {
      return Couple.of("", trimmedMatcher);
    }

    String methodMatcher = trimmedMatcher.substring(0, index);
    String paramsMatcher = trimmedMatcher.substring(index);

    return Couple.of(methodMatcher.trim(), paramsMatcher.trim());
  }

  @Nullable
  private static ParamMatcher createParametersMatcher(String paramsMatcher) {
    if (paramsMatcher.length() <= 2) return null;

    String paramsString = paramsMatcher.substring(1, paramsMatcher.length() - 1);
    List<String> params = StringUtil.split(paramsString, ",").stream().map(String::trim).collect(Collectors.toList());
    if (ContainerUtil.find(params, String::isEmpty) != null) return null;

    List<StringMatcher> matchers = params.stream().map(StringMatcherBuilder::create).filter(Objects::nonNull).collect(Collectors.toList());

    if (matchers.size() == params.size()) {
      return new StringParamMatcher(matchers);
    }
    else {
      return null;
    }
  }

  @Nullable
  public static Matcher createMatcher(@Nonnull String matcher) {
    Couple<String> pair = extract(matcher);
    if (pair == null) {
      return null;
    }

    StringMatcher methodNameMatcher = StringMatcherBuilder.create(pair.first);
    if (methodNameMatcher == null) {
      return null;
    }
    ParamMatcher paramMatcher = pair.second.isEmpty() ? paramNames -> true : createParametersMatcher(pair.second);

    return paramMatcher != null ? new Matcher(methodNameMatcher, paramMatcher) : null;
  }
}
