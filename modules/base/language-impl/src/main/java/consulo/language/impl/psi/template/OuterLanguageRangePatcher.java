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
package consulo.language.impl.psi.template;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Customizes template data language-specific parsing in templates.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface OuterLanguageRangePatcher extends LanguageExtension {
  ExtensionPointCacheKey<OuterLanguageRangePatcher, ByLanguageValue<OuterLanguageRangePatcher>> KEY = ExtensionPointCacheKey.create("OuterLanguageRangePatcher", LanguageOneToOne.build());

  @Nullable
  static OuterLanguageRangePatcher forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(OuterLanguageRangePatcher.class).getOrBuildCache(KEY).get(language);
  }

  /**
   * @return Text to be inserted for parsing in outer element insertion ranges provided by
   * {@link TemplateDataElementType.RangeCollector#addOuterRange(TextRange, boolean)} where <tt>isInsertion == true</tt>
   */
  @Nullable
  String getTextForOuterLanguageInsertionRange(@Nonnull TemplateDataElementType templateDataElementType, @Nonnull CharSequence outerElementText);
}
