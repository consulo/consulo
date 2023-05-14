/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 05-Sep-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageBraceMatcher extends BraceMatcher, LanguageExtension {
  ExtensionPointCacheKey<LanguageBraceMatcher, ByLanguageValue<LanguageBraceMatcher>> KEY = ExtensionPointCacheKey.create("LanguageBraceMatcher", LanguageOneToOne.build());

  @Nullable
  static LanguageBraceMatcher forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(LanguageBraceMatcher.class).getOrBuildCache(KEY).get(language);
  }
}
