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
package consulo.language.editor.uast;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.ElementColorProvider;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * A language-independent color provider written against the UAST tree: it backs the color gutter marker
 * (the platform {@code ColorLineMarkerProvider} in ide-impl) for every language that has a UAST binding.
 * <p>
 * This is a separate extension point and deliberately does NOT implement {@link ElementColorProvider}:
 * a single UAST color provider is fanned out by {@code consulo.language.editor.uast.internal.UastElementColorProviderExtender}
 * into one {@link ElementColorProvider} per registered {@link UastLanguagePlugin}, which is what the platform
 * color gutter marker consumes.
 * <p>
 * The element handed to {@link #getColorFrom(UElement, UastLanguagePlugin)} and
 * {@link #setColorTo(UElement, ColorValue, UastLanguagePlugin)} is the UAST element directly converted from the PSI element
 * the platform asked about: the adapter only delegates when {@code uElement.getSourcePsi() == psiElement},
 * so every source element is reported at most once.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class UastElementColorProvider {
    /**
     * The UAST element types this provider is interested in.
     * Only PSI elements that convert to one of these types are handed to
     * {@link #getColorFrom(UElement, UastLanguagePlugin)} and {@link #setColorTo(UElement, ColorValue, UastLanguagePlugin)}.
     *
     * @return the UAST element types the provider handles
     */
    public abstract Class<? extends UElement>[] getUElementTypesHint();

    /**
     * Mirrors {@link ElementColorProvider#getColorFrom(consulo.language.psi.PsiElement)} for a UAST element.
     *
     * @param element the UAST element directly converted from the PSI element the platform asked about
     * @param plugin  the UAST plugin of the language the provider is currently running for
     * @return the color represented by the element, or null if the element does not represent a color
     */
    @RequiredReadAction
    public abstract @Nullable ColorValue getColorFrom(UElement element, UastLanguagePlugin plugin);

    /**
     * Mirrors {@link ElementColorProvider#setColorTo(consulo.language.psi.PsiElement, ColorValue)} for a UAST element.
     * <p>
     * The platform has no UAST code generation: the implementation must rewrite the source through the language PSI,
     * see {@link UElement#getSourcePsi()}.
     *
     * @param element the UAST element directly converted from the PSI element the platform asked about,
     *                the same element {@link #getColorFrom(UElement, UastLanguagePlugin)} returned a color for
     * @param color   the color chosen by the user
     * @param plugin  the UAST plugin of the language the provider is currently running for
     */
    @RequiredWriteAction
    public abstract void setColorTo(UElement element, ColorValue color, UastLanguagePlugin plugin);
}
