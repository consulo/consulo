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
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.uast.UastLineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import org.jspecify.annotations.Nullable;

/**
 * Adapts one {@link UastLineMarkerProvider} to one language: a regular {@link LineMarkerProvider}
 * bound to the language of the given {@link UastLanguagePlugin}.
 * Instances are created by {@link UastLineMarkerProviderExtender}, one per (provider, plugin) pair.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
public final class UastLineMarkerProviderAdapter implements LineMarkerProvider {
    private final UastLineMarkerProvider myProvider;
    private final UastLanguagePlugin myPlugin;

    public UastLineMarkerProviderAdapter(UastLineMarkerProvider provider, UastLanguagePlugin plugin) {
        myProvider = provider;
        myPlugin = plugin;
    }

    public UastLineMarkerProvider getProvider() {
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
    public boolean isAvailable(PsiFile file) {
        return myProvider.isAvailable(file);
    }

    /**
     * Converts the element to UAST (restricted to the provider's type hint) and asks the provider only when the element
     * is the source PSI of the conversion result, so that every UAST element is offered to the provider exactly once.
     */
    @Override
    @RequiredReadAction
    public @Nullable LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        UElement uElement = myPlugin.convertElementWithParent(element, myProvider.getUElementTypesHint());
        if (uElement == null || uElement.getSourcePsi() != element) {
            return null;
        }
        return myProvider.getLineMarkerInfo(uElement, myPlugin);
    }
}
