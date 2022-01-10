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

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * from kotlin
 */
public class StringMatcherBuilder {
  @Nullable
  public static StringMatcher create(@Nonnull String matcher) {
    if (matcher.isEmpty()) {
      return text -> true;
    }

    return createAsterisksMatcher(matcher);
  }

  @Nullable
  private static StringMatcher createAsterisksMatcher(String matcher) {
    int asterisksCount = StringUtil.countChars(matcher, '*');
    if (asterisksCount > 2) return null;

    if (asterisksCount == 0) {
      return text -> Comparing.equal(text, matcher);
    }

    if (matcher.equals("*")) {
      return text -> true;
    }

    if (StringUtil.startsWithChar(matcher, '*') && asterisksCount == 1) {
      String target = matcher.substring(1);
      return text -> StringUtil.endsWith(text, target);
    }

    if (StringUtil.endsWithChar(matcher, '*') && asterisksCount == 1) {
      String target = matcher.substring(0, matcher.length() - 1);
      return text -> StringUtil.startsWith(text, target);
    }

    if (StringUtil.startsWithChar(matcher, '*') && StringUtil.endsWithChar(matcher, '*')) {
      String target = matcher.substring(1, matcher.length() - 1);
      return text -> text.contains(target);
    }

    return null;
  }
}
