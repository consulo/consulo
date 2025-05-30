// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * Once the tree building started, it must provide at least one text node. Otherwise, an exception will be thrown.
 */
public interface DeclarativePresentationTreeBuilder {
    void list(Consumer<DeclarativePresentationTreeBuilder> builder);

    void collapsibleList(CollapseState state,
                         Consumer<DeclarativeCollapsiblePresentationTreeBuilder> expandedState,
                         Consumer<DeclarativeCollapsiblePresentationTreeBuilder> collapsedState);

    default void collapsibleList(Consumer<DeclarativeCollapsiblePresentationTreeBuilder> expandedState,
                                 Consumer<DeclarativeCollapsiblePresentationTreeBuilder> collapsedState) {
        collapsibleList(CollapseState.NoPreference, expandedState, collapsedState);
    }

    void icon(@Nonnull Image image);

    void text(String text, InlayActionData actionData);

    default void text(String text) {
        text(text, null);
    }

    void clickHandlerScope(InlayActionData actionData, Consumer<DeclarativePresentationTreeBuilder> builder);
}

