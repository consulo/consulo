// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl.event;

import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import javax.annotation.Nonnull;

import java.util.EventListener;

public interface MarkupModelListener extends EventListener {
  MarkupModelListener[] EMPTY_ARRAY = new MarkupModelListener[0];

  default void afterAdded(@Nonnull RangeHighlighterEx highlighter) {
  }

  default void beforeRemoved(@Nonnull RangeHighlighterEx highlighter) {
  }

  default void attributesChanged(@Nonnull RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
  }

  /**
   * @deprecated Use {@link MarkupModelListener} directly.
   */
  @Deprecated
  abstract class Adapter implements MarkupModelListener {
  }
}
