// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup;

import javax.annotation.Nonnull;

import java.util.EventListener;

/**
 * Listener to receive notifications for events in lookup.
 *
 * @see Lookup#addLookupListener(LookupListener)
 */
public interface LookupListener extends EventListener {
  default void lookupShown(@Nonnull LookupEvent event) {
  }

  /*
   * Note: this event comes inside the command that performs inserting of text into the editor and is
   * called before the lookup string is inserted into the document. If any listener returns false,
   * the lookup string is not inserted.
   */
  default boolean beforeItemSelected(@Nonnull LookupEvent event) {
    return true;
  }

  /*
   * Note: this event comes inside the command that performs inserting of text into the editor.
   */
  default void itemSelected(@Nonnull LookupEvent event) {
  }

  default void lookupCanceled(@Nonnull LookupEvent event) {
  }

  default void currentItemChanged(@Nonnull LookupEvent event) {
  }

  /**
   * Fired when the contents or the selection of the lookup list is changed (items added by
   * background calculation, selection moved by the user, etc.)
   */
  default void uiRefreshed() {
  }

  default void focusDegreeChanged() {
  }
}
