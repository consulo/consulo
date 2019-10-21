/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * Kind of file types capable to provide {@link Language}.
 */
public abstract class LanguageFileType implements FileType {
  private final Language myLanguage;
  private final boolean mySecondary;

  /**
   * Creates a language file type for the specified language.
   *
   * @param language The language used in the files of the type.
   */
  protected LanguageFileType(@Nonnull Language language) {
    this(language, false);
  }

  /**
   * Creates a language file type for the specified language.
   *
   * @param language  The language used in the files of the type.
   * @param secondary If true, this language file type will never be returned as the associated file type for the language.
   *                  (Used when a file type is reusing the language of another file type, e.g. XML).
   */
  protected LanguageFileType(@Nonnull Language language, boolean secondary) {
    // passing Language instead of lazy resolve on getLanguage call (like LazyRunConfigurationProducer), is ok because:
    // 1. Usage of FileType nearly always requires Language
    // 2. FileType is created only on demand (if deprecated FileTypeFactory is not used).
    myLanguage = language;
    mySecondary = secondary;
  }

  /**
   * Returns the language used in the files of the type.
   *
   * @return The language instance.
   */

  @Nonnull
  public final Language getLanguage() {
    return myLanguage;
  }

  public Charset extractCharsetFromFileContent(@Nullable Project project, @Nullable VirtualFile file, @Nonnull CharSequence content) {
    return null;
  }

  /**
   * If true, this language file type will never be returned as the associated file type for the language.
   * (Used when a file type is reusing the language of another file type, e.g. XML).
   */
  public boolean isSecondary() {
    return mySecondary;
  }
}
