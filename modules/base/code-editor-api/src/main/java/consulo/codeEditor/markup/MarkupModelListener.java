// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.markup;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

public interface MarkupModelListener extends EventListener {
    MarkupModelListener[] EMPTY_ARRAY = new MarkupModelListener[0];

    default void afterAdded(@Nonnull RangeHighlighterEx highlighter) {
    }

    default void beforeRemoved(@Nonnull RangeHighlighterEx highlighter) {
    }

    /**
     * Called when the {@code highlighter} is disposed.
     * Inside this method the {@code highlighter.isValid()} returns {@code false}, so some methods might be unavailable or have undefined behaviour.
     * For example, all {@code setXXX} methods, e.g., {@link RangeHighlighterEx#setTextAttributes(TextAttributes)} might fail.
     * Only getters are guaranteed to work.
     */
    default void afterRemoved(@Nonnull RangeHighlighterEx highlighter) {
    }

    default void attributesChanged(@Nonnull RangeHighlighterEx highlighter,
                                   boolean renderersChanged,
                                   boolean fontStyleOrColorChanged) {
    }

    default void attributesChanged(@Nonnull RangeHighlighterEx highlighter,
                                   boolean renderersChanged,
                                   boolean fontStyleChanged,
                                   boolean foregroundColorChanged) {
        attributesChanged(highlighter, renderersChanged, fontStyleChanged || foregroundColorChanged);
    }

}
