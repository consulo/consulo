// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.ui.search;

import consulo.configurable.SearchableConfigurable;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * A place for {@link SearchableOptionContributor} implementations to feed the searchable options to.
 *
 * @author peter
 */
public interface SearchableOptionProcessor {
  /**
   * Take text that can be found on a setting page, split it into words and add them to the internal setting search index.
   *
   * @param text                    the text that appears on a setting page and can be searched for
   * @param path                    for complex settings pages, identifies the subpage where the option is to be found.
   *                                For example, it can be the name of tab on the settings page that should be opened when showing search results.
   *                                Can be {@code null} for simple configurables.
   * @param hit                     the string that's presented to the user when showing found results in a list, e.g. in Goto Action.
   * @param configurableId          the id of the topmost configurable containing the search result. See {@link SearchableConfigurable#getId()}
   * @param configurableDisplayName display name of the configurable containing the search result
   */
  void addOption(String option, String path, String hit, String configurableId, String configurableDisplayName);

  Set<String> getProcessedWordsWithoutStemming(@Nonnull String text);

  Set<String> getProcessedWords(@Nonnull String text);
}

