// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.ui.util.ActionUtil;
import com.intellij.collaboration.ui.util.SwingBindingsUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;

@ApiStatus.Internal
public final class SimpleComboboxWithActionsFactory<T> {
    private final StateFlow<Collection<T>> myMappingsState;
    private final MutableStateFlow<T> mySelectionState;

    public SimpleComboboxWithActionsFactory(
        @Nonnull StateFlow<Collection<T>> mappingsState,
        @Nonnull MutableStateFlow<T> selectionState
    ) {
        myMappingsState = mappingsState;
        mySelectionState = selectionState;
    }

    public @Nonnull ComboBox<?> create(
        @Nonnull CoroutineScope scope,
        @Nonnull Function<T, Presentation> presenter,
        @Nonnull StateFlow<List<Action>> actions,
        @Nonnull Comparator<T> sortComparator
    ) {
        ComboBoxWithActionsModel<T> comboModel = new ComboBoxWithActionsModel<>();
        SwingBindingsUtil.bindComboBoxWithActionsModel(scope, comboModel, myMappingsState, mySelectionState, actions, sortComparator);

        if (comboModel.getSelectedItem() == null) {
            CollaborationToolsUIUtil.selectFirst(comboModel);
        }

        ComboBox<ComboBoxWithActionsModel.Item<T>> comboBox = new ComboBox<>(comboModel);
        comboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                @Nonnull JList<? extends ComboBoxWithActionsModel.Item<T>> list,
                ComboBoxWithActionsModel.Item<T> value,
                int index, boolean selected, boolean hasFocus
            ) {
                if (value instanceof ComboBoxWithActionsModel.Item.Wrapper<T> wrapper) {
                    Presentation p = presenter.apply(wrapper.wrappee());
                    append(p.name());
                    String secondaryName = p.secondaryName();
                    if (secondaryName != null) {
                        append(" ").append(secondaryName, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
                if (value instanceof ComboBoxWithActionsModel.Item.Action<T> actionItem) {
                    if (comboModel.getSize() == index) {
                        setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
                    }
                    String name = ActionUtil.getName(actionItem.getAction());
                    append(name != null ? name : "");
                }
            }
        });
        comboBox.setUsePreferredSizeAsMinimum(false);
        comboBox.setOpaque(false);
        comboBox.setSwingPopup(true);

        ComboboxSpeedSearch.installSpeedSearch(comboBox, item -> {
            if (item instanceof ComboBoxWithActionsModel.Item.Wrapper<T> wrapper) {
                return presenter.apply(wrapper.wrappee()).name();
            }
            if (item instanceof ComboBoxWithActionsModel.Item.Action<T> actionItem) {
                String name = ActionUtil.getName(actionItem.getAction());
                return name != null ? name : "";
            }
            return "";
        });

        return comboBox;
    }

    public record Presentation(@Nls @Nonnull String name, @Nls @Nullable String secondaryName) {
    }
}
