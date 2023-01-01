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
package consulo.language.editor.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 29-Jun-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageDocumentationProvider extends DocumentationProvider, LanguageExtension {
  ExtensionPointCacheKey<LanguageDocumentationProvider, ByLanguageValue<List<LanguageDocumentationProvider>>> KEY =
          ExtensionPointCacheKey.create("LanguageDocumentationProvider", LanguageOneToMany.build(false));

  @Nonnull
  static List<LanguageDocumentationProvider> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(LanguageDocumentationProvider.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nullable
  static DocumentationProvider forLanguageComposite(@Nonnull Language language) {
    List<LanguageDocumentationProvider> providers = forLanguage(language);
    if (providers.isEmpty()) {
      return null;
    }
    if (providers.size() == 1) {
      return providers.get(0);
    }
    return CompositeDocumentationProvider.wrapProviders(providers);
  }
}
