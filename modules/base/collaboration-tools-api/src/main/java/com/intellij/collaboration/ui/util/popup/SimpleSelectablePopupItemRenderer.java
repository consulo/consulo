// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util.popup;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public final class SimpleSelectablePopupItemRenderer<T> implements ListCellRenderer<T> {
    private static final int TOP_BOTTOM_GAP = 1;

    private final boolean myShowCheckbox;
    private final Function<T, SelectablePopupItemPresentation> myPresenter;
    private final JBCheckBox myCheckBox;
    private final SimpleColoredComponent myLabel;
    private final BorderLayoutPanel myPanel;

    private SimpleSelectablePopupItemRenderer(
        boolean showCheckbox,
        @Nonnull Function<T, SelectablePopupItemPresentation> presenter
    ) {
        myShowCheckbox = showCheckbox;
        myPresenter = presenter;

        myCheckBox = new JBCheckBox();
        myCheckBox.setOpaque(false);
        myCheckBox.setVisible(showCheckbox);

        myLabel = new SimpleColoredComponent();
        myLabel.setIconTextGap(JBUIScale.scale(4));

        int leftRightGap = CollaborationToolsUIUtil.getSize(5, 0); // in case of the newUI gap handled by SelectablePanel
        myPanel = new BorderLayoutPanel(6, 5);
        myPanel.addToLeft(myCheckBox);
        myPanel.addToCenter(myLabel);
        myPanel.setBorder(JBUI.Borders.empty(TOP_BOTTOM_GAP, leftRightGap));
    }

    @Override
    public @Nonnull Component getListCellRendererComponent(
        @Nonnull JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        SelectablePopupItemPresentation presentation = myPresenter.apply(value);

        myCheckBox.setSelected(presentation.isSelected());
        myCheckBox.setFocusPainted(cellHasFocus);
        myCheckBox.setFocusable(cellHasFocus);

        myLabel.clear();
        myLabel.append(presentation.getShortText());
        myLabel.setIcon(presentation.getIcon());
        myLabel.setForeground(ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus()));

        UIUtil.setBackgroundRecursively(myPanel, ListUiUtil.WithTallRow.background(list, isSelected, true));

        return myPanel;
    }

    public static <T> @Nonnull ListCellRenderer<T> create(@Nonnull Function<T, SelectablePopupItemPresentation> presenter) {
        return create(true, presenter);
    }

    public static <T> @Nonnull ListCellRenderer<T> create(
        boolean showCheckboxes,
        @Nonnull Function<T, SelectablePopupItemPresentation> presenter
    ) {
        SimpleSelectablePopupItemRenderer<T> renderer = new SimpleSelectablePopupItemRenderer<>(showCheckboxes, presenter);
        if (!ExperimentalUI.isNewUI()) {
            return renderer;
        }
        return new RoundedCellRenderer<>(renderer, false);
    }
}
