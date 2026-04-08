// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter;
import com.intellij.collaboration.ui.util.popup.*;
import com.intellij.collaboration.util.IncrementallyComputedValue;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Function;

import static com.intellij.collaboration.ui.util.popup.PopupExtensions.showAndAwaitSubmission;
import static com.intellij.collaboration.ui.util.popup.PopupExtensions.showAndAwaitSubmissions;

/**
 * Utility class for displaying chooser popups with various loading mechanisms.
 * <p>
 * This utility supports both preloaded item display and dynamically loaded item display,
 * with options for customization in appearance and filtering behavior.
 */
public final class ChooserPopupUtil {
    private ChooserPopupUtil() {
    }

    /**
     * Shows a chooser popup with preloaded items.
     *
     * @param point     the point at which to show the popup
     * @param items     the complete list of items to display
     * @param presenter a function that creates a {@link PopupItemPresentation} for each item
     * @return the selected item, or {@code null} if the popup was cancelled
     */
    public static <T> @Nullable T showChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull List<T> items,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        return showChooserPopup(point, items, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Shows a chooser popup with preloaded items.
     */
    public static <T> @Nullable T showChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull List<T> items,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig
    ) {
        return showChooserPopup(
            point,
            items,
            filterByNamesFromPresentation(presenter),
            SimplePopupItemRenderer.create(presenter::apply),
            popupConfig
        );
    }

    /**
     * Shows a chooser popup with preloaded items.
     */
    public static <T> @Nullable T showChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull List<T> items,
        @Nonnull Function<T, String> filteringMapper,
        @Nonnull ListCellRenderer<T> renderer,
        @Nonnull PopupConfig popupConfig
    ) {
        CollectionListModel<T> listModel = new CollectionListModel<>(items);
        JBList<T> list = createList(listModel, renderer);

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> builder = new PopupChooserBuilder<>(list);
        builder.setFilteringEnabled(item -> filteringMapper.apply((T) item));
        configure(builder, popupConfig);
        JBPopup popup = builder.createPopup();

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());
        return showAndAwaitSubmission(popup, list, point, popupConfig.getShowDirection());
    }

    /**
     * Shows a chooser popup that loads items progressively.
     */
    public static <T> @Nullable T showAsyncChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull Flow<kotlin.Result<List<T>>> itemsLoader,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        return showAsyncChooserPopup(point, itemsLoader, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Shows a chooser popup that loads items progressively.
     */
    public static <T> @Nullable T showAsyncChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull Flow<kotlin.Result<List<T>>> itemsLoader,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig
    ) {
        return showAsyncChooserPopup(
            point,
            itemsLoader,
            filterByNamesFromPresentation(presenter),
            SimplePopupItemRenderer.create(presenter::apply),
            popupConfig
        );
    }

    /**
     * Shows a chooser popup that loads items progressively.
     */
    public static <T> @Nullable T showAsyncChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull Flow<kotlin.Result<List<T>>> itemsLoader,
        @Nonnull Function<T, String> filteringMapper,
        @Nonnull ListCellRenderer<T> renderer,
        @Nonnull PopupConfig popupConfig
    ) {
        CollectionListModel<T> listModel = new CollectionListModel<>();
        JBList<T> list = createList(listModel, renderer);
        com.intellij.util.ui.LaunchOnShowKt.launchOnShow(
            list,
            "List items loader",
            (scope, continuation) -> {
                list.setPaintBusy(true);
                list.getEmptyText().clear();
                try {
                    kotlinx.coroutines.flow.FlowKt.collect(itemsLoader, resultedItems -> {
                            // Handle Result
                            try {
                                List<T> items = kotlin.ResultKt.getOrThrow(resultedItems);
                                int selected = list.getSelectedIndex();
                                if (items.size() > listModel.getSize()) {
                                    List<T> newList = items.subList(listModel.getSize(), items.size());
                                    listModel.addAll(listModel.getSize(), newList);
                                }
                                if (selected != -1) {
                                    list.setSelectedIndex(selected);
                                }
                            }
                            catch (Throwable exception) {
                                showError(list.getEmptyText(), exception, popupConfig.getErrorPresenter());
                            }
                            return kotlin.Unit.INSTANCE;
                        },
                        continuation
                    );
                }
                finally {
                    list.setPaintBusy(false);
                }
                return kotlin.Unit.INSTANCE;
            }
        );

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> builder = new PopupChooserBuilder<>(list);
        builder.setFilteringEnabled(item -> filteringMapper.apply((T) item));
        configure(builder, popupConfig);
        JBPopup popup = builder.createPopup();

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());

        return showAndAwaitSubmission(popup, list, point, popupConfig.getShowDirection());
    }

    /**
     * Shows a chooser popup with custom configuration.
     */
    @ApiStatus.Internal
    public static <T> @Nullable T showAsyncChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig,
        @Nonnull ListCellRenderer<T> renderer,
        @Nonnull PopupConfigureCallback<T> configure
    ) {
        CollectionListModel<T> listModel = new CollectionListModel<>();
        JBList<T> list = createList(listModel, renderer);

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> popupBuilder = new PopupChooserBuilder<>(list);
        popupBuilder.setFilteringEnabled(item -> presenter.apply((T) item).getShortText());
        configure(popupBuilder, popupConfig);

        popupBuilder.setCancelOnOtherWindowOpen(false);
        popupBuilder.setCancelOnWindowDeactivation(false);

        JBPopup popup = popupBuilder.createPopup();

        configure.configure(popup, list, listModel, popupBuilder.getScrollPane());

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());
        return showAndAwaitSubmission(popup, list, point, popupConfig.getShowDirection());
    }

    /**
     * Displays a chooser popup with incremental loading of items.
     */
    public static <T> @Nullable T showChooserPopupWithIncrementalLoading(
        @Nonnull RelativePoint point,
        @Nonnull StateFlow<IncrementallyComputedValue<List<T>>> listState,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        return showChooserPopupWithIncrementalLoading(point, listState, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Displays a chooser popup with incremental loading of items.
     */
    public static <T> @Nullable T showChooserPopupWithIncrementalLoading(
        @Nonnull RelativePoint point,
        @Nonnull StateFlow<IncrementallyComputedValue<List<T>>> listState,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig
    ) {
        CollectionListModel<T> listModel = new CollectionListModel<>();
        JBList<T> list = createList(listModel, SimplePopupItemRenderer.create(presenter::apply));
        com.intellij.util.ui.LaunchOnShowKt.launchOnShow(list, "List items loader", (scope, continuation) -> {
            kotlinx.coroutines.flow.FlowKt.collect(listState, state -> {
                list.setPaintBusy(state.isLoading());

                Throwable exception = state.getExceptionOrNull();
                if (exception != null) {
                    showError(list.getEmptyText(), exception, popupConfig.getErrorPresenter());
                }
                else {
                    list.getEmptyText().clear();
                }

                // Handle value availability
                IncrementallyComputedValue.onNoValue(
                    state,
                    () -> {
                        listModel.removeAll();
                        return kotlin.Unit.INSTANCE;
                    }
                );
                IncrementallyComputedValue.onValueAvailable(
                    state,
                    newList -> {
                        T selected = list.getSelectedValue();
                        listModel.replaceAll(newList);
                        list.setSelectedValue(selected, true);
                        return kotlin.Unit.INSTANCE;
                    }
                );
                return kotlin.Unit.INSTANCE;
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> builder = new PopupChooserBuilder<>(list);
        builder.setFilteringEnabled(item -> presenter.apply((T) item).getShortText());
        configure(builder, popupConfig);
        JBPopup popup = builder.createPopup();

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());
        return showAndAwaitSubmission(popup, list, point, popupConfig.getShowDirection());
    }

    /**
     * Displays a chooser popup that allows selecting multiple items with incremental loading.
     */
    @ApiStatus.Internal
    public static <T> @Nonnull List<T> showMultipleChooserPopupWithIncrementalLoading(
        @Nonnull RelativePoint point,
        @Nonnull List<T> currentItems,
        @Nonnull StateFlow<IncrementallyComputedValue<List<T>>> listState,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        return showMultipleChooserPopupWithIncrementalLoading(point, currentItems, listState, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Displays a chooser popup that allows selecting multiple items with incremental loading.
     */
    @ApiStatus.Internal
    public static <T> @Nonnull List<T> showMultipleChooserPopupWithIncrementalLoading(
        @Nonnull RelativePoint point,
        @Nonnull List<T> currentItems,
        @Nonnull StateFlow<IncrementallyComputedValue<List<T>>> listState,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig
    ) {
        MultiChooserListModel<T> listModel = new MultiChooserListModel<>();
        listModel.add(currentItems);
        listModel.setChosen(currentItems);
        JBList<T> list = createSelectableList(listModel, presenter);

        com.intellij.util.ui.LaunchOnShowKt.launchOnShow(list, "List items loader", (scope, continuation) -> {
            kotlinx.coroutines.flow.FlowKt.collect(listState, state -> {
                    list.setPaintBusy(state.isLoading());

                    Throwable exception = state.getExceptionOrNull();
                    if (exception != null) {
                        showError(list.getEmptyText(), exception, popupConfig.getErrorPresenter());
                    }
                    else {
                        list.getEmptyText().clear();
                    }

                    com.intellij.collaboration.util.IncrementallyComputedValueKt.onNoValue(
                        state,
                        () -> {
                            listModel.removeAllExceptChosen();
                            return kotlin.Unit.INSTANCE;
                        }
                    );
                    IncrementallyComputedValue.onValueAvailable(
                        state,
                        newList -> {
                            T selected = list.getSelectedValue();
                            listModel.retainChosenAndUpdate(newList);
                            list.setSelectedValue(selected, true);
                            return kotlin.Unit.INSTANCE;
                        }
                    );
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            );
            return kotlin.Unit.INSTANCE;
        });

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> builder = new PopupChooserBuilder<>(list);
        builder.setFilteringEnabled(item -> filterByNamesFromPresentation(presenter).apply((T) item));
        builder.setCloseOnEnter(false);
        configure(builder, popupConfig);
        JBPopup popup = builder.createPopup();

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());

        return showAndAwaitSubmissions(popup, listModel, point, popupConfig.getShowDirection());
    }

    /**
     * Displays an asynchronous popup allowing users to select multiple items.
     */
    @ApiStatus.Internal
    public static <T> @Nonnull List<T> showAsyncMultipleChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull List<T> selectedItems,
        @Nonnull Flow<kotlin.Result<List<T>>> loadedBatchesFlow,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        return showAsyncMultipleChooserPopup(point, selectedItems, loadedBatchesFlow, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Displays an asynchronous popup allowing users to select multiple items.
     */
    @ApiStatus.Internal
    public static <T> @Nonnull List<T> showAsyncMultipleChooserPopup(
        @Nonnull RelativePoint point,
        @Nonnull List<T> selectedItems,
        @Nonnull Flow<kotlin.Result<List<T>>> loadedBatchesFlow,
        @Nonnull Function<T, PopupItemPresentation> presenter,
        @Nonnull PopupConfig popupConfig
    ) {
        MultiChooserListModel<T> listModel = new MultiChooserListModel<>();
        listModel.add(selectedItems);
        listModel.setChosen(selectedItems);
        JBList<T> list = createSelectableList(listModel, presenter);
        com.intellij.util.ui.LaunchOnShowKt.launchOnShow(list, "List items loader", (scope, continuation) -> {
            list.setPaintBusy(true);
            list.getEmptyText().clear();
            try {
                kotlinx.coroutines.flow.FlowKt.collect(loadedBatchesFlow, resultedItems -> {
                    try {
                        List<T> items = kotlin.ResultKt.getOrThrow(resultedItems);
                        int selected = list.getSelectedIndex();
                        if (items.size() > listModel.getSize()) {
                            List<T> newList = items.subList(listModel.getSize(), items.size());
                            listModel.add(newList);
                        }
                        if (selected != -1) {
                            list.setSelectedIndex(selected);
                        }
                    }
                    catch (Throwable exception) {
                        showError(list.getEmptyText(), exception, popupConfig.getErrorPresenter());
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            }
            finally {
                list.setPaintBusy(false);
            }
            return kotlin.Unit.INSTANCE;
        });

        @SuppressWarnings("unchecked")
        PopupChooserBuilder<T> builder = new PopupChooserBuilder<>(list);
        builder.setFilteringEnabled(selectableItem -> presenter.apply((T) selectableItem).getShortText());
        builder.setCloseOnEnter(false);
        configure(builder, popupConfig);
        JBPopup popup = builder.createPopup();
        // non-empty list returns pref size without considering "visibleRowCount"
        popup.getContent().setPreferredSize(new JBDimension(250, 300));

        CollaborationToolsPopupUtil.configureSearchField(popup, popupConfig);
        PopupUtil.setPopupToggleComponent(popup, point.getComponent());

        return showAndAwaitSubmissions(popup, listModel, point, popupConfig.getShowDirection());
    }

    private static <T> @Nonnull JBList<T> createList(@Nonnull ListModel<T> listModel, @Nonnull ListCellRenderer<T> renderer) {
        JBList<T> list = new JBList<>(listModel);
        list.setVisibleRowCount(7);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(renderer);
        list.setBackground(JBUI.CurrentTheme.Popup.BACKGROUND);
        return list;
    }

    private static <T> @Nonnull JBList<T> createSelectableList(
        @Nonnull MultiChooserListModel<T> model,
        @Nonnull Function<T, PopupItemPresentation> presenter
    ) {
        ListCellRenderer<T> rendererWithChooser = SimpleSelectablePopupItemRenderer.create(item -> {
            PopupItemPresentation presentation = presenter.apply(item);
            return new SelectablePopupItemPresentation.Simple(
                presentation.getShortText(),
                presentation.getIcon(),
                presentation.getFullText(),
                model.isChosen(item)
            );
        });
        JBList<T> list = createList(model, rendererWithChooser);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED) && !UIUtil.isSelectionButtonDown(e) && !e.isConsumed()) {
                    toggleSelectedChosen(list, model);
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e != null && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    toggleSelectedChosen(list, model);
                }
            }
        });
        return list;
    }

    private static <T> void toggleSelectedChosen(@Nonnull JBList<T> list, @Nonnull MultiChooserListModel<T> model) {
        int idx = list.getSelectedIndex();
        if (idx >= 0) {
            model.toggleChosen(idx);
            // due to a bug in FilteringListModel the changed item is not redrawn on change, so we have to do it manually
            Rectangle cellBounds = list.getCellBounds(idx, idx);
            if (cellBounds != null) {
                list.repaint(cellBounds);
            }
        }
    }

    private static <T> void configure(@Nonnull PopupChooserBuilder<T> builder, @Nonnull PopupConfig popupConfig) {
        String title = popupConfig.getTitle();
        if (title != null) {
            builder.setTitle(title);
        }

        builder.setFilterAlwaysVisible(popupConfig.isAlwaysShowSearchField());
        builder.setMovable(popupConfig.isMovable());
        builder.setResizable(popupConfig.isResizable());
        builder.setAutoPackHeightOnFiltering(popupConfig.isAutoPackHeightOnFiltering());
    }

    private static <T> @Nonnull Function<T, String> filterByNamesFromPresentation(@Nonnull Function<T, PopupItemPresentation> presenter) {
        return item -> {
            PopupItemPresentation presentation = presenter.apply(item);
            StringBuilder sb = new StringBuilder();
            sb.append(presentation.getShortText());
            if (presentation.getFullText() != null) {
                sb.append(" ").append(presentation.getFullText());
            }
            return sb.toString();
        };
    }

    static void showError(
        @Nonnull StatusText statusText,
        @Nonnull Throwable e,
        @Nullable ErrorStatusPresenter.Text<Throwable> errorPresenter
    ) {
        if (errorPresenter == null) {
            String errorMessage = e.getLocalizedMessage();
            if (errorMessage == null) {
                errorMessage = CollaborationToolsLocalize.popupDataLoadingError().get();
            }
            statusText.setText(errorMessage, SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
        else {
            Action errorAction = errorPresenter.getErrorAction(e);
            statusText.appendText(errorPresenter.getErrorTitle(e), SimpleTextAttributes.ERROR_ATTRIBUTES);
            if (errorAction != null) {
                String actionName = (String) errorAction.getValue(Action.NAME);
                statusText.appendSecondaryText(
                    actionName != null ? actionName : "",
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    errorAction
                );
            }
        }
    }

    @FunctionalInterface
    public interface PopupConfigureCallback<T> {
        void configure(
            @Nonnull JBPopup popup,
            @Nonnull JBList<T> list,
            @Nonnull CollectionListModel<T> listModel,
            @Nonnull JScrollPane scrollPane
        );
    }
}
