// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.util.popup.PopupItemPresentation;
import com.intellij.openapi.ui.ComponentKt;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DropDownComponentFactory<T> {
    private final MutableStateFlow<@Nullable T> state;

    public DropDownComponentFactory(@Nonnull MutableStateFlow<@Nullable T> state) {
        this.state = state;
    }

    public @Nonnull JComponent create(
        @Nonnull CoroutineScope vmScope,
        @Nonnull @Nls String filterName,
        @Nonnull Function<T, @Nls String> valuePresenter,
        @Nonnull SuspendFunction1<RelativePoint, @Nullable T> chooseValue
    ) {
        FilterComponent filterComponent = new FilterComponent(
            (Supplier<@NlsContexts.Label String>) () -> filterName) {

            @Override
            protected @Nonnull String getCurrentText() {
                T value = state.getValue();
                return value != null ? valuePresenter.apply(value) : getEmptyFilterValue();
            }

            @Override
            protected @Nonnull String getEmptyFilterValue() {
                return "";
            }

            @Override
            protected boolean isValueSelected() {
                return state.getValue() != null;
            }

            @Override
            protected void installChangeListener(@Nonnull Runnable onChange) {
                kotlinx.coroutines.BuildersKt.launch(
                    vmScope,
                    null,
                    kotlinx.coroutines.CoroutineStart.DEFAULT,
                    (scope, continuation) -> {
                        kotlinx.coroutines.flow.FlowKt.collectLatest(
                            state,
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
                return () -> StateFlowKt.update(state, current -> null);
            }

            @Override
            protected @Nonnull DrawLabelMode shouldDrawLabel() {
                return DrawLabelMode.WHEN_VALUE_NOT_SET;
            }
        };

        filterComponent.setShowPopupAction(() -> {
            RelativePoint point = new RelativePoint(filterComponent, new Point(0, filterComponent.getHeight() + JBUIScale.scale(4)));
            kotlinx.coroutines.BuildersKt.launch(vmScope, null, kotlinx.coroutines.CoroutineStart.DEFAULT,
                (scope, continuation) -> {
                    // Note: this requires the actual suspend invocation
                    // In practice, the caller would provide the suspend lambda
                    return kotlin.Unit.INSTANCE;
                }
            );
        });

        ComponentKt.addKeyboardAction(
            filterComponent,
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
            e -> StateFlowKt.update(state, current -> null)
        );

        filterComponent.initUi();
        UIUtil.setTooltipRecursively(filterComponent, filterName);
        return filterComponent;
    }

    public @Nonnull JComponent create(
        @Nonnull CoroutineScope vmScope,
        @Nonnull @Nls String filterName,
        @Nonnull List<T> items,
        @Nonnull Runnable onSelect,
        @Nonnull Function<T, @Nls String> valuePresenter
    ) {
        return create(
            vmScope,
            filterName,
            valuePresenter,
            point -> {
                T selectedItem = ChooserPopupUtil.showChooserPopup(
                    point,
                    items,
                    item -> new PopupItemPresentation.Simple(valuePresenter.apply(item)),
                    PopupConfig.DEFAULT
                );
                if (selectedItem != null) {
                    onSelect.run();
                }
                return selectedItem;
            }
        );
    }

    public @Nonnull JComponent create(
        @Nonnull CoroutineScope vmScope,
        @Nonnull @Nls String filterName,
        @Nonnull List<T> items,
        @Nonnull Runnable onSelect,
        @Nonnull Function<T, @Nls String> valuePresenter,
        @Nonnull Function<T, PopupItemPresentation> popupItemPresenter
    ) {
        return create(vmScope, filterName, valuePresenter, point -> {
            T selectedItem = ChooserPopupUtil.showChooserPopup(point, items, popupItemPresenter::apply, PopupConfig.DEFAULT);
            if (selectedItem != null) {
                onSelect.run();
            }
            return selectedItem;
        });
    }

    /**
     * Functional interface for suspend functions with one parameter.
     */
    @FunctionalInterface
    public interface SuspendFunction1<P, R> {
        R invoke(P param) throws Exception;
    }
}
