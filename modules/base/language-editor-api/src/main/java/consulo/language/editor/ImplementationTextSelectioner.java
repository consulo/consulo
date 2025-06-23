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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.internal.DefaultImplementationTextSelectioner;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2008-02-01
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ImplementationTextSelectioner extends LanguageExtension {
    ExtensionPointCacheKey<ImplementationTextSelectioner, ByLanguageValue<ImplementationTextSelectioner>> KEY =
        ExtensionPointCacheKey.create("ImplementationTextSelectioner", LanguageOneToOne.build(new DefaultImplementationTextSelectioner()));

    @Nullable
    static ImplementationTextSelectioner forLanguage(Language language) {
        ExtensionPoint<ImplementationTextSelectioner> extensionPoint =
            Application.get().getExtensionPoint(ImplementationTextSelectioner.class);
        ByLanguageValue<ImplementationTextSelectioner> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(language);
    }

    @RequiredReadAction
    int getTextStartOffset(@Nonnull PsiElement element);

    @RequiredReadAction
    int getTextEndOffset(@Nonnull PsiElement element);
}