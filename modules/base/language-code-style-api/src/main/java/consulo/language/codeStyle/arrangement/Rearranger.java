/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsAware;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Encapsulates language-specific rearrangement logic.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 *
 * @param <E> entry type
 * @author Denis Zhdanov
 * @since 7/16/12 3:23 PM
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface Rearranger<E extends ArrangementEntry> extends LanguageExtension {
    ExtensionPointCacheKey<Rearranger, ByLanguageValue<Rearranger>> KEY =
        ExtensionPointCacheKey.create("Rearranger", LanguageOneToOne.build());

    @Nullable
    static Rearranger forLanguage(Language language) {
        ExtensionPoint<Rearranger> extensionPoint = Application.get().getExtensionPoint(Rearranger.class);
        ByLanguageValue<Rearranger> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(language);
    }

    /**
     * Tries to wrap given element into arrangement entry at the target context.
     * <p/>
     * This is useful in a situation when new element is generated and we're deciding where to insert it (e.g. new field is
     * generated and we want to insert it according to the arrangement rules like 'fields before methods').
     *
     * @param element  element to wrap into format eligible for further processing by arrangement engine
     * @param settings arrangement settings to use. The primary idea is to make the rearranger aware about
     *                 {@link StdArrangementTokens.Grouping grouping rules} (if any). E.g. it's not worth to process java method bodies
     *                 in order to build method dependency graph if no such grouping rule is defined
     * @return arrangement entry for the given element if it's possible to perform the mapping and list of arrangement entries
     * available at the given context plus newly created entry for the given element;
     * <code>null</code> otherwise
     */
    @Nullable
    Pair<E, List<E>> parseWithNew(
        @Nonnull PsiElement root,
        @Nullable Document document,
        @Nonnull Collection<TextRange> ranges,
        @Nonnull PsiElement element,
        @Nonnull ArrangementSettings settings
    );

    /**
     * Allows to build rearranger-interested data for the given element.
     *
     * @param root     root element which children should be parsed for the rearrangement
     * @param document document which corresponds to the target PSI tree
     * @param ranges   target offsets ranges to use for filtering given root's children
     * @param settings arrangement settings to use. The primary idea is to make the rearranger aware about
     *                 {@link StdArrangementTokens.Grouping grouping rules} (if any). E.g. it's not worth to process java method bodies
     *                 in order to build method dependency graph if no such grouping rule is defined
     * @return given root's children which are subject for further rearrangement
     */
    @Nonnull
    List<E> parse(
        @Nonnull PsiElement root,
        @Nullable Document document,
        @Nonnull Collection<TextRange> ranges,
        @Nonnull ArrangementSettings settings
    );

    /**
     * Allows to answer how many blank lines should be inserted before the target arrangement entry which position is changed.
     *
     * @param settings code style settings to use (it's assumed that returned result is derived from 'blank lines' code style settings)
     * @param parent   target entry's parent (if available)
     * @param previous previous entry (if available)
     * @param target   target entry which blank lines number the caller is interested in
     * @return number of blank lines to insert before the target entry;
     * negative as an indication that no blank lines adjustment is necessary
     */
    int getBlankLines(@Nonnull CodeStyleSettings settings, @Nullable E parent, @Nullable E previous, @Nonnull E target);


    /**
     * @return serializer to save {@link ArrangementSettings arrangement settings}.
     * Serializer is expected to be lazy and don't save
     * {@link ArrangementStandardSettingsAware.getDefaultSettings() default settings}.
     * <p/>
     * @see DefaultArrangementSettingsSerializer
     */
    @Nonnull
    ArrangementSettingsSerializer getSerializer();
}
