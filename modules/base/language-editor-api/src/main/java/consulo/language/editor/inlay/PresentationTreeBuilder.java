// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import java.util.function.Consumer;

/**
 * Once the tree building started, it must provide at least one text node. Otherwise, an exception will be thrown.
 */
public interface PresentationTreeBuilder {
    void list(Consumer<PresentationTreeBuilder> builder);

    void collapsibleList(CollapseState state,
                         Consumer<CollapsiblePresentationTreeBuilder> expandedState,
                         Consumer<CollapsiblePresentationTreeBuilder> collapsedState);

    default void collapsibleList(Consumer<CollapsiblePresentationTreeBuilder> expandedState,
                                 Consumer<CollapsiblePresentationTreeBuilder> collapsedState) {
        collapsibleList(CollapseState.NoPreference, expandedState, collapsedState);
    }

    void text(String text, InlayActionData actionData);

    default void text(String text) {
        text(text, null);
    }

    void clickHandlerScope(InlayActionData actionData, Consumer<PresentationTreeBuilder> builder);
}

