// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.intention;

import consulo.language.editor.completion.lookup.Lookup;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * KeymapGroupImpl of intention actions with specific title.
 * <p>
 * IntentionActionWithChoice can be used to reduce number of user's
 * clicks.
 * <p>
 * For example, it can add variants right into quickfix popup eliminating
 * need for user to click on quickfix and then to choose specific variation
 * in {@link Lookup}.
 *
 * @see com.intellij.spellchecker.quickfixes.ChangeTo for reference implementation
 * @see DefaultIntentionActionWithChoice for reasonably preconfigured action
 */
public interface IntentionActionWithChoice<T extends IntentionAction, V extends IntentionAction> {
  /**
   * Title that should be used in UI for this group
   * of actions
   */
  @Nonnull
  T getTitle();

  /**
   * Variants that will be rendered in UI.
   * <p>
   * Not, that if you need variants to maintain specific order,
   * you'll have to implement Comparable for T.
   */
  @Nonnull
  List<V> getVariants();
}
