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
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.fileTypes.*;
import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author nik
 */
@Singleton
public class FileNameMatcherFactoryImpl extends FileNameMatcherFactory {
  @Nonnull
  @Override
  public FileNameMatcher createExtensionFileNameMatcher(@Nonnull String extension) {
    return new ExtensionFileNameMatcher(extension);
  }

  @Nonnull
  @Override
  public FileNameMatcher createExactFileNameMatcher(@Nonnull String fileName, boolean ignoreCase) {
    return new ExactFileNameMatcher(fileName, ignoreCase);
  }

  @Nonnull
  @Override
  public FileNameMatcher createWildcardFileNameMatcher(@Nonnull String pattern) {
    return new WildcardFileNameMatcher(pattern);
  }
}
