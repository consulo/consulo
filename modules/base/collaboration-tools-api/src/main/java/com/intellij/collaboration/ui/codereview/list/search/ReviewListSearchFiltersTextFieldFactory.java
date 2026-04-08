// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.util.BindingsKt;
import consulo.ide.IdeBundle;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.keymap.util.KeymapUtil;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class ReviewListSearchFiltersTextFieldFactory {
    private ReviewListSearchFiltersTextFieldFactory() {
    }

    static @Nonnull JTextField create(
        @Nonnull Flow<@Nullable String> searchTextFlow,
        @Nullable Consumer<String> setSearchText,
        @Nonnull Consumer<@Nullable String> submitSearchText,
        @Nonnull SuspendConsumer<RelativePoint> chooseFromHistory
    ) {
        ExtendableTextField searchField = new ExtendableTextField();
        searchField.addActionListener(e -> {
            String text = searchField.getText();
            submitSearchText.accept(text != null && !text.isEmpty() ? text : null);
        });

        LaunchOnShowKt.launchOnShow(searchField, "ReviewListTextField", (scope, continuation) -> {
            Flow<String> mapped = FlowKt.map(searchTextFlow, (value, cont) -> {
                return value != null && !value.isEmpty() ? value : "";
            });
            BindingsKt.bindTextIn(searchField, scope, mapped, newText -> {
                if (setSearchText != null) {
                    setSearchText.accept(newText);
                }
            });
            return kotlin.Unit.INSTANCE;
        });

        Runnable[] onShowHistoryCallback = new Runnable[]{null};
        LaunchOnShowKt.launchOnShow(
            searchField,
            "ReviewListTextFieldHistoryPoint",
            (scope, continuation) -> {
                onShowHistoryCallback[0] = () -> {
                    RelativePoint point = createHistoryPoint(searchField);
                    kotlinx.coroutines.BuildersKt.launch(
                        scope,
                        null,
                        kotlinx.coroutines.CoroutineStart.DEFAULT,
                        (innerScope, innerCont) -> {
                            chooseFromHistory.accept(point);
                            return kotlin.Unit.INSTANCE;
                        }
                    );
                };
                try {
                    kotlinx.coroutines.AwaitCancellationKt.awaitCancellation(continuation);
                }
                finally {
                    onShowHistoryCallback[0] = null;
                }
                return kotlin.Unit.INSTANCE;
            }
        );

        searchField.addExtension(new ExtendableTextComponent.Extension() {
            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public @Nonnull Icon getIcon(boolean hovered) {
                return AllIcons.Actions.SearchWithHistory;
            }

            @Override
            public @Nonnull Runnable getActionOnClick() {
                return () -> {
                    Runnable callback = onShowHistoryCallback[0];
                    if (callback != null) {
                        callback.run();
                    }
                };
            }
        });

        searchField.setToolTipText(
            IdeBundle.message("tooltip.search.history.hotkey", KeymapUtil.getShortcutText("ShowSearchHistory"))
        );

        DumbAwareAction.create(e -> {
            Runnable callback = onShowHistoryCallback[0];
            if (callback != null) {
                callback.run();
            }
        }).registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("ShowSearchHistory"), searchField);

        return searchField;
    }

    private static @Nonnull RelativePoint createHistoryPoint(@Nonnull ExtendableTextField searchField) {
        return new RelativePoint(searchField, new Point(JBUIScale.scale(2), searchField.getHeight()));
    }

    @FunctionalInterface
    interface SuspendConsumer<T> {
        void accept(T value) throws Exception;
    }
}
