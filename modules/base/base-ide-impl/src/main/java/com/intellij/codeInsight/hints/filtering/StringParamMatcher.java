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

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * from kotlin
 */
public class StringParamMatcher implements ParamMatcher {
  private final List<StringMatcher> myParamMatchers;

  public StringParamMatcher(List<StringMatcher> paramMatchers) {
    this.myParamMatchers = paramMatchers;
  }

  @Override
  public boolean isMatching(List<String> paramNames) {
    if (paramNames.size() != myParamMatchers.size()) {
      return false;
    }

    Iterable<Pair<StringMatcher, String>> zip = ContainerUtil.zip(myParamMatchers, paramNames);
    for (Pair<StringMatcher, String> pair : zip) {
      if (!pair.getFirst().isMatching(pair.getSecond())) {
        return false;
      }
    }
    return true;
  }
}
