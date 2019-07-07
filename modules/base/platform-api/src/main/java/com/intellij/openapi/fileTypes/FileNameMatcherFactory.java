/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class FileNameMatcherFactory {
  @Nonnull
  public static FileNameMatcherFactory getInstance() {
    return ServiceManager.getService(FileNameMatcherFactory.class);
  }

  @Nonnull
  public final FileNameMatcher createMatcher(@Nonnull String pattern) {
    if (pattern.startsWith("*.") && pattern.indexOf('*', 2) < 0 && pattern.indexOf('.', 2) < 0 && pattern.indexOf('?', 2) < 0) {
      return createExtensionFileNameMatcher(StringUtil.toLowerCase(pattern.substring(2)));
    }

    if (pattern.contains("*") || pattern.contains("?")) {
      return createWildcardFileNameMatcher(pattern);
    }

    return createExactFileNameMatcher(pattern);
  }

  @Nonnull
  public FileNameMatcher createExactFileNameMatcher(@Nonnull String fileName) {
    return createExactFileNameMatcher(fileName, false);
  }

  @Nonnull
  public abstract FileNameMatcher createExtensionFileNameMatcher(@Nonnull String extension);

  @Nonnull
  public abstract FileNameMatcher createExactFileNameMatcher(@Nonnull String fileName, boolean ignoreCase);

  @Nonnull
  public abstract FileNameMatcher createWildcardFileNameMatcher(@Nonnull String pattern);
}
