// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.HorizontalListPanel;
import com.intellij.collaboration.ui.util.popup.PopupExtensionsKt;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.JBPopupFactory;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class ReviewListSearchPanelFactory<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>,
    VM extends ReviewListSearchPanelViewModel<S, Q>> {

    protected final @Nonnull VM vm;

    protected ReviewListSearchPanelFactory(@Nonnull VM vm) {
        this.vm = vm;
    }

    public @Nonnull JComponent create(@Nonnull CoroutineScope viewScope) {
        return create(viewScope, null);
    }

    @ApiStatus.Internal
    public @Nonnull JComponent create(@Nonnull CoroutineScope viewScope, @Nullable Consumer<Q> quickFilterListener) {
        JTextField searchField = ReviewListSearchFiltersTextFieldFactory.create(
            vm.getQueryState(),
            null,
            text -> StateFlowKt.update(vm.getQueryState(), current -> text),
            point -> {
                List<S> history = vm.getSearchHistory();
                Collections.reverse(history);
                S value = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(history)
                    .setRenderer(SimpleListCellRenderer.create((label, item, index) -> label.setText(getShortText(item))))
                    .createPopup()
                    .showAndAwaitListSubmission(point, ShowDirection.BELOW);
                if (value != null) {
                    StateFlowKt.update(vm.getSearchState(), current -> value);
                }
            }
        );

        List<JComponent> filters = createFilters(viewScope);

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
            vm.getSearchState(),
            vm.getQuickFilters(),
            quickFilterListener,
            this::getQuickFilterTitle,
            newQuery -> StateFlowKt.update(vm.getSearchState(), current -> newQuery),
            () -> StateFlowKt.update(vm.getSearchState(), current -> vm.getEmptySearch())
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

    protected abstract @Nonnull @Nls String getShortText(@Nonnull S searchValue);

    protected abstract @Nonnull List<JComponent> createFilters(@Nonnull CoroutineScope viewScope);

    protected abstract @Nonnull @Nls String getQuickFilterTitle(@Nonnull Q quickFilter);
}
