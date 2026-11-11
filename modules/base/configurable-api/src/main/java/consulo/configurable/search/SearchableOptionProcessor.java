// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.configurable.search;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * Takes the texts a {@link SearchableOptionContributor} adds to the settings search. The texts are localized values:
 * the index splits them into words, and resolves them again in the locale it is built in.
 */
public interface SearchableOptionProcessor {
    /**
     * Adds the text of an option of a settings page.
     *
     * @param text             the text the option is found by - split into words by the index
     * @param hit              what a found option is shown as, e.g. in Find Action
     * @param configurableId   the {@link consulo.configurable.Configurable#getId() id} of the page of the option
     * @param configurableName the name of that page
     */
    default void addOption(LocalizeValue text, LocalizeValue hit, String configurableId, LocalizeValue configurableName) {
        addOption(text, hit, null, configurableId, configurableName);
    }

    /**
     * @param path for a page of several parts, the part the option is in - the tab to open, null for a simple page
     */
    void addOption(LocalizeValue text, LocalizeValue hit, @Nullable String path, String configurableId, LocalizeValue configurableName);
}
