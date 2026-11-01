/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.spellcheker.text;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.util.ModificationTracker;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface TextContentModificationTrackerProvider extends LanguageExtension {
    ExtensionPointCacheKey<TextContentModificationTrackerProvider, ByLanguageValue<TextContentModificationTrackerProvider>> KEY =
        ExtensionPointCacheKey.create("TextContentModificationTrackerProvider", LanguageOneToOne.build());

    static @Nullable TextContentModificationTrackerProvider forLanguage(Language language) {
        return Application.get().getExtensionPoint(TextContentModificationTrackerProvider.class).getOrBuildCache(KEY).get(language);
    }

    @Nullable
    ModificationTracker getModificationTracker(PsiElement psiElement);
}
