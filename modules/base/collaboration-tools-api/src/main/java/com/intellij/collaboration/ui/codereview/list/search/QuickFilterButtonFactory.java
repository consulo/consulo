// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import consulo.application.dumb.DumbAware;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

final class QuickFilterButtonFactory {
    private static final BadgeIconSupplier FILTER_ICON = new BadgeIconSupplier(PlatformIconGroup.generalFilter());

    private QuickFilterButtonFactory() {
    }

    static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> @Nonnull JComponent create(
        @Nonnull StateFlow<S> searchState,
        @Nonnull List<Q> quickFilters,
        @Nullable Consumer<Q> filterListener,
        @Nonnull Function<Q, @Nls String> quickFilterTitleProvider,
        @Nonnull Consumer<S> setSearchQuery,
        @Nonnull Runnable clearSearchQuery
    ) {
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
            "Review.FilterToolbar",
            new DefaultActionGroup(new FilterPopupMenuAction<>(
                searchState,
                quickFilters,
                filterListener,
                quickFilterTitleProvider,
                setSearchQuery,
                clearSearchQuery
            )),
            true
        );
        toolbar.setLayoutStrategy(ToolbarLayoutStrategy.NOWRAP_STRATEGY);
        toolbar.getComponent().setOpaque(false);
        toolbar.getComponent().setBorder(JBUI.Borders.empty());
        toolbar.setTargetComponent(null);

        LaunchOnShowKt.launchOnShow(
            toolbar.getComponent(),
            "ReviewFilterToolbarListener",
            (scope, continuation) -> {
                Disposable disposable = Disposer.newDisposable();
                try {
                    toolbar.addListener(
                        new ActionToolbarListener() {
                            @Override
                            public void actionsUpdated() {
                                UIUtil.forEachComponentInHierarchy(
                                    toolbar.getComponent(),
                                    comp -> {
                                        if (comp instanceof ActionButton) {
                                            comp.setFocusable(true);
                                        }
                                    }
                                );
                            }
                        },
                        disposable
                    );
                    kotlinx.coroutines.AwaitCancellationKt.awaitCancellation(continuation);
                }
                finally {
                    Disposer.dispose(disposable);
                }
                return kotlin.Unit.INSTANCE;
            }
        );

        LaunchOnShowKt.launchOnShow(
            toolbar.getComponent(),
            "ReviewFilterToolbar",
            (scope, continuation) -> {
                kotlinx.coroutines.flow.FlowKt.collect(searchState,
                    value -> {
                        kotlinx.coroutines.DispatchersKt.withContext(
                            Dispatchers.getEDT(),
                            (innerScope, innerCont) -> {
                                toolbar.updateActionsAsync();
                                return kotlin.Unit.INSTANCE;
                            },
                            continuation
                        );
                        return kotlin.Unit.INSTANCE;
                    }, continuation
                );
                return kotlin.Unit.INSTANCE;
            }
        );

        return toolbar.getComponent();
    }

    private static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> void showQuickFiltersPopup(
        @Nonnull JComponent parentComponent,
        @Nonnull StateFlow<S> searchState,
        @Nonnull List<Q> quickFilters,
        @Nullable Consumer<Q> filterListener,
        @Nonnull Function<Q, @Nls String> quickFilterTitleProvider,
        @Nonnull Consumer<S> setSearchQuery,
        @Nonnull Runnable clearSearchQuery
    ) {
        List<AnAction> quickFiltersActions = new ArrayList<>();
        quickFiltersActions.add(Separator.create(CollaborationToolsLocalize.reviewListFilterQuickTitle().get()));
        for (Q filter : quickFilters) {
            quickFiltersActions.add(new QuickFilterAction<>(
                searchState,
                quickFilterTitleProvider.apply(filter),
                filter,
                filterListener,
                setSearchQuery
            ));
        }
        quickFiltersActions.add(Separator.create());
        quickFiltersActions.add(new ClearFiltersAction<>(searchState, clearSearchQuery));

        JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                new DefaultActionGroup(quickFiltersActions),
                DataManager.getInstance().getDataContext(parentComponent),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            )
            .showUnderneathOf(parentComponent);
    }

    private static final class FilterPopupMenuAction<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>>
        extends DumbAwareAction implements DumbAware {

        private final StateFlow<S> searchState;
        private final List<Q> quickFilters;
        private final Consumer<Q> filterListener;
        private final Function<Q, String> quickFilterTitleProvider;
        private final Consumer<S> setSearchQuery;
        private final Runnable clearSearchQuery;

        FilterPopupMenuAction(
            @Nonnull StateFlow<S> searchState,
            @Nonnull List<Q> quickFilters,
            @Nullable Consumer<Q> filterListener,
            @Nonnull Function<Q, String> quickFilterTitleProvider,
            @Nonnull Consumer<S> setSearchQuery,
            @Nonnull Runnable clearSearchQuery
        ) {
            super(CollaborationToolsLocalize.reviewListFilterQuickTitle());
            this.searchState = searchState;
            this.quickFilters = quickFilters;
            this.filterListener = filterListener;
            this.quickFilterTitleProvider = quickFilterTitleProvider;
            this.setSearchQuery = setSearchQuery;
            this.clearSearchQuery = clearSearchQuery;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setIcon(FILTER_ICON.getLiveIndicatorIcon(searchState.getValue().getFilterCount() != 0));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            showQuickFiltersPopup(
                (JComponent) e.getInputEvent().getComponent(),
                searchState,
                quickFilters,
                filterListener,
                quickFilterTitleProvider,
                setSearchQuery,
                clearSearchQuery
            );
        }
    }

    private static final class QuickFilterAction<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>>
        extends DumbAwareAction implements Toggleable {

        private final StateFlow<S> searchState;
        private final Q filter;
        private final Consumer<Q> filterListener;
        private final Consumer<S> setSearchQuery;

        QuickFilterAction(
            @Nonnull StateFlow<S> searchState,
            @Nonnull @Nls String name,
            @Nonnull Q filter,
            @Nullable Consumer<Q> filterListener,
            @Nonnull Consumer<S> setSearchQuery
        ) {
            super(name);
            this.searchState = searchState;
            this.filter = filter;
            this.filterListener = filterListener;
            this.setSearchQuery = setSearchQuery;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Toggleable.setSelected(e.getPresentation(), searchState.getValue().equals(filter.getFilter()));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (filterListener != null) {
                filterListener.accept(filter);
            }
            setSearchQuery.accept(filter.getFilter());
        }
    }

    private static final class ClearFiltersAction<S extends ReviewListSearchValue> extends DumbAwareAction {
        private final StateFlow<S> searchState;
        private final Runnable clearSearchQuery;

        ClearFiltersAction(
            @Nonnull StateFlow<S> searchState,
            @Nonnull Runnable clearSearchQuery
        ) {
            super(CollaborationToolsLocalize.reviewListFilterQuickClear(searchState.getValue().getFilterCount()).get());
            this.searchState = searchState;
            this.clearSearchQuery = clearSearchQuery;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(searchState.getValue().getFilterCount() > 0);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            clearSearchQuery.run();
        }
    }
}
