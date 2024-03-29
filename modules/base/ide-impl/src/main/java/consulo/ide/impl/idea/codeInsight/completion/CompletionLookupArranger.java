// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.CompletionResult;
import consulo.language.editor.completion.CompletionSorter;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.WeighingContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Determines the order of completion items and the initial selection.
 *
 * @author yole
 */
public interface CompletionLookupArranger {
  Key<WeighingContext> WEIGHING_CONTEXT = Key.create("WEIGHING_CONTEXT");
  Key<Integer> PREFIX_CHANGES = Key.create("PREFIX_CHANGES");

  /**
   * Adds an element to be arranged.
   *
   * @param presentation The presentation of the element (rendered with {@link LookupElement#renderElement(LookupElementPresentation)}
   */
  void addElement(@Nonnull LookupElement element, @Nonnull CompletionSorter sorter, @Nonnull PrefixMatcher prefixMatcher, @Nonnull LookupElementPresentation presentation);

  /**
   * Adds an element to be arranged, along with its prefix matcher.
   */
  void addElement(@Nonnull CompletionResult result);

  /**
   * Returns the prefix matcher registered for the specified element.
   */
  PrefixMatcher itemMatcher(@Nonnull LookupElement item);

  /**
   * Returns the items in the appropriate order and the initial selection.
   *
   * @return Pair where the first element is the sorted list of completion items and the second item is the index of the item to select
   * initially.
   */
  Pair<List<LookupElement>, Integer> arrangeItems();
}
