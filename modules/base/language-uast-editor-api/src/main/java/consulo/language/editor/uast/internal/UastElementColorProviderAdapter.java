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
package consulo.language.editor.uast.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.Language;
import consulo.language.editor.uast.UastElementColorProvider;
import consulo.language.psi.ElementColorProvider;
import consulo.language.psi.PsiElement;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * Adapts one {@link UastElementColorProvider} to one language: a regular {@link ElementColorProvider}
 * bound to the language of the given {@link UastLanguagePlugin}.
 * Instances are created by {@link UastElementColorProviderExtender}, one per (provider, plugin) pair.
 * <p>
 * Only PSI elements which are directly converted to UAST ({@code uElement.getSourcePsi() == element}) are delegated,
 * so that a color is reported once per source element and not again for every parent the conversion reaches.
 * <p>
 * The platform color gutter marker is registered for {@link Language#ANY} and asks every {@link ElementColorProvider}
 * about every PSI element, so elements of a foreign language are rejected before the plugin is asked to convert them.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
public final class UastElementColorProviderAdapter implements ElementColorProvider {
    private final UastElementColorProvider myProvider;
    private final UastLanguagePlugin myPlugin;

    public UastElementColorProviderAdapter(UastElementColorProvider provider, UastLanguagePlugin plugin) {
        myProvider = provider;
        myPlugin = plugin;
    }

    public UastElementColorProvider getProvider() {
        return myProvider;
    }

    public UastLanguagePlugin getPlugin() {
        return myPlugin;
    }

    @Override
    public Language getLanguage() {
        return myPlugin.getLanguage();
    }

    @Override
    @RequiredReadAction
    public @Nullable ColorValue getColorFrom(PsiElement element) {
        UElement uElement = convertDirect(element);
        if (uElement == null) {
            return null;
        }
        return myProvider.getColorFrom(uElement, myPlugin);
    }

    @Override
    @RequiredWriteAction
    public void setColorTo(PsiElement element, ColorValue color) {
        UElement uElement = convertDirect(element);
        if (uElement == null) {
            return;
        }
        myProvider.setColorTo(uElement, color, myPlugin);
    }

    /**
     * @return the UAST element directly converted from the given PSI element (restricted to the provider's type hint),
     * or null when the element belongs to another language, does not convert or converts only through one of its parents
     */
    private @Nullable UElement convertDirect(PsiElement element) {
        if (!element.getLanguage().isKindOf(myPlugin.getLanguage())) {
            return null;
        }
        UElement uElement = myPlugin.convertElementWithParent(element, myProvider.getUElementTypesHint());
        if (uElement == null || uElement.getSourcePsi() != element) {
            return null;
        }
        return uElement;
    }
}
