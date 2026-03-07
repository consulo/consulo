// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.flow.FlowKt;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReviewListSearchFiltersPanelFactory {
    private ReviewListSearchFiltersPanelFactory() {
    }

    public static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> @Nonnull JComponent create(
        @Nonnull ReviewListSearchFiltersPanelViewModel<S, Q> vm,
        @Nonnull Function<S, @Nls String> getShortText,
        @Nonnull Supplier<List<JComponent>> createFilters,
        @Nonnull Function<Q, @Nls String> getQuickFilterTitle
    ) {
        return create(vm, getShortText, createFilters, getQuickFilterTitle, null);
    }

    @ApiStatus.Internal
    public static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> @Nonnull JComponent create(
        @Nonnull ReviewListSearchFiltersPanelViewModel<S, Q> vm,
        @Nonnull Function<S, @Nls String> getShortText,
        @Nonnull Supplier<List<JComponent>> createFilters,
        @Nonnull Function<Q, @Nls String> getQuickFilterTitle,
        @Nullable Consumer<Q> quickFilterListener
    ) {
        kotlinx.coroutines.flow.Flow<String> searchQueryTextState = FlowKt.map(
            vm.getSearchQueryState(),
            (value, cont) -> value.getSearchQuery()
        );

        JTextField searchField = ReviewListSearchFiltersTextFieldFactory.create(
            searchQueryTextState,
            vm::setSearchText,
            text -> vm.submitSearchText(),
            point -> {
                List<S> history = vm.getSearchHistory();
                Collections.reverse(history);
                S value = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(history)
                    .setRenderer(SimpleListCellRenderer.create((label, item, index) -> label.setText(getShortText.apply(item))))
                    .createPopup()
                    .showAndAwaitListSubmission(point, ShowDirection.BELOW);
                if (value != null) {
                    vm.setSearchQuery(value);
                }
            }
        );

        List<JComponent> filters = createFilters.get();

        HorizontalListPanel filtersInnerPanel = new HorizontalListPanel(4);
        for (JComponent filter : filters) {
            filtersInnerPanel.add(filter);
        }

        JScrollPane filtersPanel = ScrollPaneFactory.createScrollPane(filtersInnerPanel, true);
        filtersPanel.setViewport(new GradientViewport(filtersInnerPanel, JBUI.insets(0, 10), false));
        filtersPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        filtersPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        filtersPanel.setHorizontalScrollBar(new JBThinOverlappingScrollBar(Adjustable.HORIZONTAL));
        ClientProperty.put(filtersPanel, JBScrollPane.FORCE_HORIZONTAL_SCROLL, true);

        JComponent quickFilterButton = QuickFilterButtonFactory.create(
            vm.getSearchQueryState(),
            vm.getQuickFilters(),
            quickFilterListener,
            getQuickFilterTitle::apply,
            vm::setSearchQuery,
            vm::clearSearchQuery
        );

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBorder(JBUI.Borders.emptyTop(10));
        filterPanel.setOpaque(false);
        filterPanel.add(quickFilterButton, BorderLayout.WEST);
        filterPanel.add(filtersPanel, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(JBUI.Borders.empty(8, 10, 0, 10));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(filterPanel, BorderLayout.SOUTH);

        return searchPanel;
    }
}
