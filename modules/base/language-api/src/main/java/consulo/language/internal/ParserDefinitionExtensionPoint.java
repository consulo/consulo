/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.internal;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionValueCache;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.parser.ParserDefinition;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-09-13
 */
public class ParserDefinitionExtensionPoint {
    private static final Key<ExtensionValueCache<ParserDefinition>> VALUE_KEY = Key.create("ParserDefinitionValue");

    private static final ExtensionPointCacheKey<ParserDefinition, ByLanguageValue<ParserDefinition>> KEY =
        ExtensionPointCacheKey.create("ParserDefinition", LanguageOneToOne.build());

    @Nullable
    public static ParserDefinition forLanguage(@Nonnull Application application, @Nonnull Language language) {
        ExtensionValueCache<ParserDefinition> cache = language.getUserData(VALUE_KEY);
        if (cache != null) {
            return cache.get();
        }

        ExtensionPoint<ParserDefinition> extensionPoint = application.getExtensionPoint(ParserDefinition.class);
        ParserDefinition definition = extensionPoint.getOrBuildCache(KEY).get(language);
        language.putUserData(VALUE_KEY, new ExtensionValueCache<>(extensionPoint, definition));
        return definition;
    }
}
