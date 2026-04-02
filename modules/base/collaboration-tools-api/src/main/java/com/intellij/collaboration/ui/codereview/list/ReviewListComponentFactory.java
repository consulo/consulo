// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import com.intellij.collaboration.ui.util.JListHoveredRowMaterialiser;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.function.Function;

public final class ReviewListComponentFactory<T> {
    private final ListModel<T> listModel;

    public ReviewListComponentFactory(@Nonnull ListModel<T> listModel) {
        this.listModel = listModel;
    }

    public @Nonnull JBList<T> create(@Nonnull Function<T, ReviewListItemPresentation> itemPresenter) {
        JBList<T> list = createList(itemPresenter, new ReviewListCellUiOptions());
        JListHoveredRowMaterialiser.install(list, new ReviewListCellRenderer<>(itemPresenter));
        return list;
    }

    @ApiStatus.Internal
    public @Nonnull JBList<T> create(
        @Nonnull Function<T, ReviewListItemPresentation> itemPresenter,
        @Nonnull ReviewListCellUiOptions options
    ) {
        JBList<T> list = createList(itemPresenter, options);
        JListHoveredRowMaterialiser.HoveredRowMaterialiser<?> materialiser =
            JListHoveredRowMaterialiser.install(list, new ReviewListCellRenderer<>(itemPresenter, options));
        materialiser.resetCellBoundsOnHover = options.isBordered();
        return list;
    }

    @ApiStatus.Internal
    private @Nonnull JBList<T> createList(
        @Nonnull Function<T, ReviewListItemPresentation> itemPresenter,
        @Nonnull ReviewListCellUiOptions options
    ) {
        ReviewListCellRenderer<T> listCellRenderer = new ReviewListCellRenderer<>(itemPresenter, options);
        JBList<T> list = new JBList<>(listModel);
        list.getEmptyText().clear();
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(listCellRenderer);
        list.setExpandableItemsEnabled(false);

        ScrollingUtil.installActions(list);
        ListUiUtil.Selection.installSelectionOnFocus(list);
        ListUiUtil.Selection.installSelectionOnRightClick(list);
        UIUtil.addNotInHierarchyComponents(list, List.of(listCellRenderer));
        return list;
    }
}
