// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.JBCurrentTheme;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;

public final class GroupedRenderer<T> implements ListCellRenderer<T> {
    private final ListCellRenderer<T> baseRenderer;
    private final BiFunction<T, Integer, Boolean> hasSeparatorAbove;
    private final BiFunction<T, Integer, Boolean> hasSeparatorBelow;
    private final SeparatorBuilder<T> buildSeparator;

    private BorderLayoutPanel contentWithSeparators;

    @ApiStatus.Internal
    public GroupedRenderer(
        @Nonnull ListCellRenderer<T> baseRenderer,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorAbove,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorBelow,
        @Nonnull SeparatorBuilder<T> buildSeparator
    ) {
        this.baseRenderer = baseRenderer;
        this.hasSeparatorAbove = hasSeparatorAbove;
        this.hasSeparatorBelow = hasSeparatorBelow;
        this.buildSeparator = buildSeparator;
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        Component content = baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        boolean sepAbove = hasSeparatorAbove.apply(value, index);
        boolean sepBelow = hasSeparatorBelow.apply(value, index);

        if (!sepAbove && !sepBelow) {
            return content;
        }

        if (contentWithSeparators == null) {
            contentWithSeparators = new BorderLayoutPanel();
        }

        contentWithSeparators.setBackground(list != null ? list.getBackground() : null);
        contentWithSeparators.removeAll();
        contentWithSeparators.addToCenter(content);
        if (sepAbove) {
            contentWithSeparators.addToTop(buildSeparator.build(value, index, SeparatorPosition.ABOVE));
        }
        if (sepBelow) {
            contentWithSeparators.addToBottom(buildSeparator.build(value, index, SeparatorPosition.BELOW));
        }

        return contentWithSeparators;
    }

    public enum SeparatorPosition {
        ABOVE,
        BELOW
    }

    @FunctionalInterface
    public interface SeparatorBuilder<T> {
        @Nonnull
        JComponent build(T value, int index, @Nonnull SeparatorPosition position);
    }

    @Nonnull
    public static <T> ListCellRenderer<T> create(
        @Nonnull ListCellRenderer<T> baseRenderer,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorAbove,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorBelow,
        @Nonnull SeparatorBuilder<T> buildSeparator
    ) {
        return new GroupedRenderer<>(baseRenderer, hasSeparatorAbove, hasSeparatorBelow, buildSeparator);
    }

    @Nonnull
    public static <T> ListCellRenderer<T> create(
        @Nonnull ListCellRenderer<T> baseRenderer,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorAbove,
        @Nonnull BiFunction<T, Integer, Boolean> hasSeparatorBelow
    ) {
        return new GroupedRenderer<>(baseRenderer, hasSeparatorAbove, hasSeparatorBelow,
            (value, index, position) -> createDefaultSeparator(null, false)
        );
    }

    @Nonnull
    public static <T> ListCellRenderer<T> create(@Nonnull ListCellRenderer<T> baseRenderer) {
        return create(baseRenderer, (v, i) -> false, (v, i) -> false);
    }

    public static @Nonnull GroupHeaderSeparator createDefaultSeparator(@Nullable @NlsContexts.Separator String text, boolean paintLine) {
        var labelInsets = ExperimentalUI.isNewUI() ? Popup.separatorLabelInsets() : JBCurrentTheme.ActionsList.cellPadding();
        GroupHeaderSeparator separator = new GroupHeaderSeparator(labelInsets);
        if (text != null) {
            separator.setHideLine(!paintLine);
            separator.setCaption(text);
        }
        return separator;
    }

    public static @Nonnull GroupHeaderSeparator createDefaultSeparator() {
        return createDefaultSeparator(null, false);
    }
}
