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
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiFile;
import consulo.language.uast.UAnchorOwner;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import org.jspecify.annotations.Nullable;

/**
 * A language-independent line marker provider written against the UAST tree.
 * <p>
 * This is a separate extension point and deliberately does NOT implement {@link LineMarkerProvider}:
 * a single UAST provider is fanned out by {@code consulo.language.editor.uast.internal.UastLineMarkerProviderExtender} into one
 * {@code UastLineMarkerProviderAdapter} per registered {@link UastLanguagePlugin}, so that the provider is registered
 * as a regular {@link LineMarkerProvider} for every language that has a UAST binding.
 * <p>
 * The element passed to {@link #getLineMarkerInfo(UElement, UastLanguagePlugin)} is the UAST element directly converted from
 * the PSI element offered by the line-markers pass, restricted to {@link #getUElementTypesHint()}. The provider is asked only when
 * {@code uElement.getSourcePsi() == psiElement}, so every UAST element (and thus every marker) is offered exactly once,
 * even when several PSI elements convert to the same UAST node.
 * <p>
 * Like plain {@link LineMarkerProvider}s, the returned marker should be anchored on a LEAF element which is as small as
 * possible, otherwise the marker is invalidated (and blinks) whenever the whole element is not fully visible.
 * For declarations anchor the marker on {@code ((UAnchorOwner) element).getUastAnchor().getSourcePsi()},
 * never on the source PSI of the whole declaration.
 *
 * @author VISTALL
 * @since 2026-09-18
 * @see UAnchorOwner
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class UastLineMarkerProvider {
    /**
     * The UAST element types this provider is interested in.
     * Only PSI elements that convert to one of these types are fed to {@link #getLineMarkerInfo(UElement, UastLanguagePlugin)}.
     *
     * @return the UAST element types the provider handles
     */
    public abstract Class<? extends UElement>[] getUElementTypesHint();

    /**
     * Mirrors {@link LineMarkerProvider#isAvailable(PsiFile)}: allows to disable this provider for the given file,
     * for example when the file belongs to a module without the required module extension.
     *
     * @param file the file the line markers are collected for
     * @return true if the provider should be queried for the elements of the file
     */
    public boolean isAvailable(PsiFile file) {
        return true;
    }

    /**
     * Get the line marker for the given UAST element.
     * <p>
     * The element is directly converted from the PSI element the line-markers pass asked for, see the class documentation
     * for the anchoring rules the returned marker must follow.
     *
     * @param element the UAST element, matching one of {@link #getUElementTypesHint()}
     * @param plugin  the UAST plugin of the language the provider is currently running for
     * @return the marker for the element, or null if there is none
     */
    @RequiredReadAction
    public abstract @Nullable LineMarkerInfo<?> getLineMarkerInfo(UElement element, UastLanguagePlugin plugin);
}
