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
package consulo.language.editor.refactoring;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.refactoring.internal.DefaultNamesValidator;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Instances of NamesValidator are obtained from {@link Language} instance.
 * An instance encapsulates knowledge of identifier rules and keyword set of the language.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface NamesValidator extends LanguageExtension {
  ExtensionPointCacheKey<NamesValidator, ByLanguageValue<NamesValidator>> KEY = ExtensionPointCacheKey.create("NamesValidator", LanguageOneToOne.build(new DefaultNamesValidator()));

  @Nullable
  static NamesValidator forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(NamesValidator.class).getOrBuildCache(KEY).get(language);
  }

  /**
   * Checks if the specified string is a keyword in the custom language.
   *
   * @param name    the string to check.
   * @param project the project in the context of which the check is done.
   * @return true if the string is a keyword, false otherwise.
   */
  boolean isKeyword(String name, Project project);

  /**
   * Checks if the specified string is a valid identifier in the custom language.
   *
   * @param name    the string to check.
   * @param project the project in the context of which the check is done.
   * @return true if the string is a valid identifier, false otherwise.
   */
  boolean isIdentifier(String name, Project project);
}
