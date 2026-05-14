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
package consulo.language.file;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2022-07-02
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageFileViewProviderFactory extends FileViewProviderFactory, LanguageExtension {
  ExtensionPointCacheKey<LanguageFileViewProviderFactory, ByLanguageValue<LanguageFileViewProviderFactory>> KEY =
    ExtensionPointCacheKey.create("LanguageFileViewProviderFactory", LanguageOneToOne.build());

  static @Nullable LanguageFileViewProviderFactory forLanguage(Language language) {
    return Application.get().getExtensionPoint(LanguageFileViewProviderFactory.class).getOrBuildCache(KEY).get(language);
  }
}
