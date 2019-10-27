// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.ex;

import com.intellij.openapi.editor.Editor;
import javax.annotation.Nonnull;

import java.awt.event.FocusEvent;
import java.util.EventListener;

/**
 * @author max
 */
public interface FocusChangeListener extends EventListener {
  void focusGained(@Nonnull Editor editor);

  default void focusLost(@Nonnull Editor editor) {
  }

  default void focusLost(@Nonnull Editor editor, @SuppressWarnings("unused") @Nonnull FocusEvent event) {
    focusLost(editor);
  }

  default void focusGained(@Nonnull Editor editor, @SuppressWarnings("unused") @Nonnull FocusEvent event) {
    focusGained(editor);
  }
}