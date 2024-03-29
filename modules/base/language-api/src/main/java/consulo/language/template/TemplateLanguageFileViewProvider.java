/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.template;

import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.ast.IElementType;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public interface TemplateLanguageFileViewProvider extends FileViewProvider {

  /**
   * e.g. JSP
   *
   * @return instanceof {@link TemplateLanguage}
   */
  @Override
  @Nonnull
  Language getBaseLanguage();

  /**
   * e.g. HTML for JSP files
   *
   * @return not instanceof {@link consulo.ide.impl.idea.lang.DependentLanguage}
   */
  @Nonnull
  Language getTemplateDataLanguage();

  /**
   * Should return content type that is used to override file content type for template data language.
   * It is required for template language injections to override non-base language content type properly
   *
   * @param language for which we want to create a file
   * @return content element type for non-base language, null otherwise
   */
  default IElementType getContentElementType(@Nonnull Language language) {
    return null;
  }
}
