// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.util.popup.PopupItemPresentation;
import com.intellij.openapi.ui.ComponentKt;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.FilterComponent;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReviewListSearchFiltersDropDownComponentFactory {
    private ReviewListSearchFiltersDropDownComponentFactory() {
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull StateFlow<@Nullable T> filterState,
        @Nonnull @Nls String filterName,
        @Nonnull Function<T, @Nls String> valuePresenter,
        @Nonnull SuspendConsumer<RelativePoint> chooseFilterValue,
        @Nonnull Runnable clearFilterValue
    ) {
        FilterComponent filterComponent = new FilterComponent(
            (Supplier<@NlsContexts.Label String>) () -> filterName) {

            @Override
            protected @Nonnull String getCurrentText() {
                T value = filterState.getValue();
                return value != null ? valuePresenter.apply(value) : getEmptyFilterValue();
            }

            @Override
            protected @Nonnull String getEmptyFilterValue() {
                return "";
            }

            @Override
            protected boolean isValueSelected() {
                return filterState.getValue() != null;
            }

            @Override
            protected void installChangeListener(@Nonnull Runnable onChange) {
                LaunchOnShowKt.launchOnShow(ReviewListSearchFiltersDropDownComponentFactory.this_(), "ReviewListDropDown",
                    (scope, continuation) -> {
                        kotlinx.coroutines.flow.FlowKt.collectLatest(
                            filterState,
                            value -> {
                                onChange.run();
                                return kotlin.Unit.INSTANCE;
                            },
                            continuation
                        );
                        return kotlin.Unit.INSTANCE;
                    }
                );
            }

            @Override
            protected @Nonnull Runnable createResetAction() {
                return clearFilterValue;
            }

            @Override
            protected @Nonnull DrawLabelMode shouldDrawLabel() {
                return DrawLabelMode.WHEN_VALUE_NOT_SET;
            }
        };

        LaunchOnShowKt.launchOnShow(filterComponent, "ReviewListDropDownPopup", (scope, continuation) -> {
            filterComponent.setShowPopupAction(() -> {
                RelativePoint point = new RelativePoint(filterComponent, new Point(0, filterComponent.getHeight() + JBUIScale.scale(4)));
                kotlinx.coroutines.BuildersKt.launch(scope, null, kotlinx.coroutines.CoroutineStart.DEFAULT,
                    (innerScope, innerCont) -> {
                        chooseFilterValue.accept(point);
                        return kotlin.Unit.INSTANCE;
                    }
                );
            });
            try {
                kotlinx.coroutines.AwaitCancellationKt.awaitCancellation(continuation);
            }
            finally {
                filterComponent.setShowPopupAction(() -> {
                });
            }
            return kotlin.Unit.INSTANCE;
        });

        ComponentKt.addKeyboardAction(
            filterComponent,
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
            e -> clearFilterValue.run()
        );

        filterComponent.initUi();
        UIUtil.setTooltipRecursively(filterComponent, filterName);
        return filterComponent;
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull StateFlow<@Nullable T> filterState,
        @Nonnull @Nls String filterName,
        @Nonnull List<T> items,
        @Nonnull Runnable onSelect,
        @Nonnull Function<T, @Nls String> valuePresenter,
        @Nonnull Function<T, PopupItemPresentation> popupItemPresenter,
        @Nonnull SuspendConsumer<@Nullable T> chooseFilterValue,
        @Nonnull Runnable clearFilterValue
    ) {
        return create(
            filterState,
            filterName,
            valuePresenter,
            point -> {
                T selectedItem = ChooserPopupUtil.showChooserPopup(point, items, popupItemPresenter::apply);
                if (selectedItem != null) {
                    onSelect.run();
                }
                chooseFilterValue.accept(selectedItem);
            },
            clearFilterValue
        );
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull StateFlow<@Nullable T> filterState,
        @Nonnull @Nls String filterName,
        @Nonnull List<T> items,
        @Nonnull Runnable onSelect,
        @Nonnull Function<T, @Nls String> valuePresenter,
        @Nonnull SuspendConsumer<@Nullable T> chooseFilterValue,
        @Nonnull Runnable clearFilterValue
    ) {
        return create(
            filterState,
            filterName,
            items,
            onSelect,
            valuePresenter,
            popupItem -> new PopupItemPresentation.Simple(valuePresenter.apply(popupItem)),
            chooseFilterValue,
            clearFilterValue
        );
    }

    // Helper to get 'this' reference for the factory class in anonymous inner class context
    private static JComponent this_() {
        throw new UnsupportedOperationException("Should not be called directly");
    }

    @FunctionalInterface
    public interface SuspendConsumer<T> {
        void accept(T value) throws Exception;
    }
}
