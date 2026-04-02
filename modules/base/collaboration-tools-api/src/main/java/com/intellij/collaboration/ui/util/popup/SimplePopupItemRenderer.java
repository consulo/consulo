// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util.popup;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBUIScale;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Function;

public final class SimplePopupItemRenderer<T> extends ColoredListCellRenderer<T> {
    private final Function<T, PopupItemPresentation> myPresenter;

    private SimplePopupItemRenderer(@Nonnull Function<T, PopupItemPresentation> presenter) {
        myPresenter = presenter;
        setIconTextGap(JBUIScale.scale(4));
    }

    @Override
    protected void customizeCellRenderer(@Nonnull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
        PopupItemPresentation presentation = myPresenter.apply(value);
        setIcon(presentation.getIcon());
        String fullText = presentation.getFullText();
        if (fullText != null) {
            append(fullText);
            append(" ");
            append(presentation.getShortText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        else {
            append(presentation.getShortText());
        }

        // ColoredListCellRenderer sets null for a background in case of !selected, so it can't work with SelectablePanel
        if (!selected) {
            setBackground(list.getBackground());
        }
    }

    public static <T> @Nonnull ListCellRenderer<T> create(@Nonnull Function<T, PopupItemPresentation> presenter) {
        SimplePopupItemRenderer<T> simplePopupItemRenderer = new SimplePopupItemRenderer<>(presenter);
        if (!ExperimentalUI.isNewUI()) {
            return simplePopupItemRenderer;
        }

        simplePopupItemRenderer.getIpad().left = 0;
        simplePopupItemRenderer.getIpad().right = 0;
        return new RoundedCellRenderer<>(simplePopupItemRenderer, false);
    }
}
