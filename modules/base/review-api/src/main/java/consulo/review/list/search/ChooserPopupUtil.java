// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.list.search;

import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.list.error.TextErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.IncrementallyComputedValue;
import consulo.review.util.ReviewUIUtil;
import consulo.review.util.popup.CollaborationToolsPopupUtil;
import consulo.review.util.popup.PopupItemPresentation;
import consulo.review.util.popup.SelectablePopupItemPresentation;
import consulo.review.util.popup.SimplePopupItemRenderer;
import consulo.review.util.popup.SimpleSelectablePopupItemRenderer;
import consulo.ui.RelativePoint2D;
import consulo.ui.RenderItem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemRenderer;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Choosers of a value, or of several values, from a list which may still be loading.
 * <p>
 * Every chooser is offered twice: {@code createXxx} builds the popup without showing it, for a control which shows the
 * popup it is given, and {@code showXxx} shows it at a point. The result completes when the popup is closed, with
 * {@code null} or the chosen values so far when nothing was chosen; cancelling the result closes the popup.
 */
public final class ChooserPopupUtil {
    private static final int MULTIPLE_CHOOSER_MIN_WIDTH = 250;

    private ChooserPopupUtil() {
    }

    /**
     * Shows a chooser popup with preloaded items.
     *
     * @param point       the point at which to show the popup
     * @param items       the complete list of items to display
     * @param presenter   a function that creates a {@link PopupItemPresentation} for each item
     * @param popupConfig configuration options for the popup appearance and behavior
     * @return the selected item, or {@code null} if the popup was cancelled
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showChooserPopup(RelativePoint2D point,
                                                                    List<T> items,
                                                                    Function<T, PopupItemPresentation> presenter,
                                                                    PopupConfig popupConfig) {
        return ChooserPopupUtil.<@Nullable T>show(point, createChooserPopup(items, presenter, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showChooserPopup(RelativePoint2D point,
                                                                    List<T> items,
                                                                    Function<T, PopupItemPresentation> presenter) {
        return showChooserPopup(point, items, presenter, PopupConfig.DEFAULT);
    }

    /**
     * Shows a chooser popup with preloaded items.
     *
     * @param point           the point at which to show the popup
     * @param items           the complete list of items to display
     * @param filteringMapper a function that extracts a filterable string from each item
     * @param renderer        the renderer for displaying items in the list
     * @param popupConfig     configuration options for the popup appearance and behavior
     * @return the selected item, or {@code null} if the popup was cancelled
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showChooserPopup(RelativePoint2D point,
                                                                    List<T> items,
                                                                    Function<T, String> filteringMapper,
                                                                    TextItemRenderer<T> renderer,
                                                                    PopupConfig popupConfig) {
        return ChooserPopupUtil.<@Nullable T>show(point, createChooserPopup(items, filteringMapper, renderer, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<@Nullable T> createChooserPopup(List<T> items,
                                                                 Function<T, PopupItemPresentation> presenter,
                                                                 PopupConfig popupConfig) {
        return createChooserPopup(items, filterByNamesFromPresentation(presenter), SimplePopupItemRenderer.create(presenter), popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<@Nullable T> createChooserPopup(List<T> items,
                                                                 Function<T, String> filteringMapper,
                                                                 TextItemRenderer<T> renderer,
                                                                 PopupConfig popupConfig) {
        return createSingleChooser(new ArrayList<>(items), filteringMapper, renderer, popupConfig, step -> {
        });
    }

    // Async choosers:

    /**
     * Shows a chooser popup that loads items progressively.
     * <p>
     * The {@code itemsLoader} is given a sink of <b>accumulated lists</b> - each one contains all items loaded so far,
     * not just the new items - and returns the future of the whole loading, which fails with the reason the items
     * could not be loaded. The loading is cancelled when the popup is closed.
     *
     * @param point       the point at which to show the popup
     * @param itemsLoader the loader of the accumulated lists of items
     * @param presenter   a function that creates a {@link PopupItemPresentation} for each item
     * @param popupConfig configuration options for the popup appearance and behavior
     * @return the selected item, or {@code null} if the popup was cancelled
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showAsyncChooserPopup(RelativePoint2D point,
                                                                         Function<Consumer<List<T>>, CompletableFuture<?>> itemsLoader,
                                                                         Function<T, PopupItemPresentation> presenter,
                                                                         PopupConfig popupConfig) {
        return ChooserPopupUtil.<@Nullable T>show(point, createAsyncChooserPopup(itemsLoader, presenter, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showAsyncChooserPopup(RelativePoint2D point,
                                                                         Function<Consumer<List<T>>, CompletableFuture<?>> itemsLoader,
                                                                         Function<T, String> filteringMapper,
                                                                         TextItemRenderer<T> renderer,
                                                                         PopupConfig popupConfig) {
        return ChooserPopupUtil.<@Nullable T>show(point, createAsyncChooserPopup(itemsLoader, filteringMapper, renderer, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<@Nullable T> createAsyncChooserPopup(Function<Consumer<List<T>>, CompletableFuture<?>> itemsLoader,
                                                                      Function<T, PopupItemPresentation> presenter,
                                                                      PopupConfig popupConfig) {
        return createAsyncChooserPopup(itemsLoader, filterByNamesFromPresentation(presenter), SimplePopupItemRenderer.create(presenter),
            popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<@Nullable T> createAsyncChooserPopup(Function<Consumer<List<T>>, CompletableFuture<?>> itemsLoader,
                                                                      Function<T, String> filteringMapper,
                                                                      TextItemRenderer<T> renderer,
                                                                      PopupConfig popupConfig) {
        List<T> items = new ArrayList<>();
        return createSingleChooser(new ArrayList<>(), filteringMapper, renderer, popupConfig,
            step -> loadAccumulated(step, itemsLoader, popupConfig, newItems -> {
                if (newItems.size() > items.size()) {
                    items.addAll(newItems.subList(items.size(), newItems.size()));
                }
                return items;
            }));
    }

    /**
     * Displays a chooser popup with incremental loading of items, allowing the user to select an item
     * from a dynamically updated list. The list is managed via an observable state, and supports filtering and custom rendering.
     *
     * @param point       A reference to the relative point where the popup will be displayed.
     * @param listState   The state of the incrementally computed list of items.
     * @param presenter   A function that takes a list item and returns its presentation details,
     *                    such as a short text, an icon, or a full description.
     * @param popupConfig Configuration options for the popup, including its behavior, appearance, and additional features.
     * @return The user-selected item from the list, or null if no item is selected.
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showChooserPopupWithIncrementalLoading(
        RelativePoint2D point,
        ObservableValue<IncrementallyComputedValue<List<T>>> listState,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        return ChooserPopupUtil.<@Nullable T>show(point, createChooserPopupWithIncrementalLoading(listState, presenter, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> CompletableFuture<@Nullable T> showChooserPopupWithIncrementalLoading(
        RelativePoint2D point,
        ObservableValue<IncrementallyComputedValue<List<T>>> listState,
        Function<T, PopupItemPresentation> presenter
    ) {
        return showChooserPopupWithIncrementalLoading(point, listState, presenter, PopupConfig.DEFAULT);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<@Nullable T> createChooserPopupWithIncrementalLoading(
        ObservableValue<IncrementallyComputedValue<List<T>>> listState,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        return createSingleChooser(new ArrayList<>(), presenter.andThen(PopupItemPresentation::getShortText),
            SimplePopupItemRenderer.create(presenter), popupConfig,
            step -> ReviewUIUtil.bind(step.popup, listState, state -> {
                AtomicReference<List<T>> items = new AtomicReference<>(List.of());
                state.onNoValue(() -> items.set(List.of())).onValueAvailable(items::set);
                step.step.setItems(items.get(), statusOf(state, popupConfig));
            }));
    }

    // Multiple options:

    /**
     * Displays a chooser popup that allows selecting multiple items from a list, with support for incremental loading.
     *
     * @param point        The location where the popup will be shown as a relative point.
     * @param currentItems The list of currently available items that should be initially displayed in the chooser.
     * @param listState    The state of incrementally loaded values for the items in the popup.
     *                     It provides new items, tracks loading status, and contains any exceptions encountered.
     * @param presenter    A function that maps each item in the list to a {@link PopupItemPresentation}, which determines how the
     *                     item will appear in the popup (e.g., text, icon).
     * @param popupConfig  A {@link PopupConfig} to configure the popup's behavior and appearance.
     * @return A list of items selected by the user from the popup.
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<List<T>> showMultipleChooserPopupWithIncrementalLoading(
        RelativePoint2D point,
        List<T> currentItems,
        ObservableValue<IncrementallyComputedValue<List<T>>> listState,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        return show(point, createMultipleChooserPopupWithIncrementalLoading(currentItems, listState, presenter, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<List<T>> createMultipleChooserPopupWithIncrementalLoading(
        List<T> currentItems,
        ObservableValue<IncrementallyComputedValue<List<T>>> listState,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        MultiChooserListModel<T> chooserModel = new MultiChooserListModel<>();
        chooserModel.add(currentItems);
        chooserModel.setChosen(currentItems);

        return createMultipleChooser(chooserModel, presenter, popupConfig, step -> ReviewUIUtil.bind(step.popup, listState, state -> {
            state.onNoValue(chooserModel::removeAllExceptChosen).onValueAvailable(chooserModel::retainChosenAndUpdate);
            step.step.setItems(chooserModel.getItems(), statusOf(state, popupConfig));
        }));
    }

    /**
     * Displays an asynchronous popup allowing users to select multiple items from a dynamically loaded list.
     *
     * @param point             The screen coordinate where the popup will be displayed.
     * @param selectedItems     A list of items that will be pre-selected when the popup is shown.
     * @param loadedBatchesFlow The loader of the accumulated lists of items, see {@link #showAsyncChooserPopup}.
     * @param presenter         A function that maps each item to its presentation details, such as text and icon.
     * @param popupConfig       Configuration options for the popup, including dimensions, behavior, and error handling.
     * @return A list of items the user has selected.
     */
    @RequiredUIAccess
    public static <T> CompletableFuture<List<T>> showAsyncMultipleChooserPopup(
        RelativePoint2D point,
        List<T> selectedItems,
        Function<Consumer<List<T>>, CompletableFuture<?>> loadedBatchesFlow,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        return show(point, createAsyncMultipleChooserPopup(selectedItems, loadedBatchesFlow, presenter, popupConfig), popupConfig);
    }

    @RequiredUIAccess
    public static <T> ChooserPopup<List<T>> createAsyncMultipleChooserPopup(
        List<T> selectedItems,
        Function<Consumer<List<T>>, CompletableFuture<?>> loadedBatchesFlow,
        Function<T, PopupItemPresentation> presenter,
        PopupConfig popupConfig
    ) {
        MultiChooserListModel<T> chooserModel = new MultiChooserListModel<>();
        chooserModel.add(selectedItems);
        chooserModel.setChosen(selectedItems);

        return createMultipleChooser(chooserModel, presenter, popupConfig, step -> loadAccumulated(step, loadedBatchesFlow, popupConfig,
            newItems -> {
                if (newItems.size() > chooserModel.getSize()) {
                    chooserModel.add(newItems.subList(chooserModel.getSize(), newItems.size()));
                }
                return chooserModel.getItems();
            }));
    }

    @RequiredUIAccess
    private static <R extends @Nullable Object> CompletableFuture<R> show(RelativePoint2D point, ChooserPopup<R> chooser, PopupConfig popupConfig) {
        CollaborationToolsPopupUtil.showPopup(chooser.popup(), point, popupConfig.showDirection());
        return chooser.result();
    }

    private record Created<T>(ListPopup popup, ChooserPopupStep<T> step) {
    }

    @RequiredUIAccess
    private static <T> ChooserPopup<@Nullable T> createSingleChooser(List<Object> initialItems,
                                                                   Function<T, String> filteringMapper,
                                                                   TextItemRenderer<T> renderer,
                                                                   PopupConfig popupConfig,
                                                                   Consumer<Created<T>> configure) {
        AtomicReference<@Nullable T> chosen = new AtomicReference<>();
        MutableFlatDataModel<Object> model = FlatDataModel.of(initialItems);
        ChooserPopupStep<T> step = new ChooserPopupStep<>(popupConfig.title(), model, filteringMapper, null, chosen::set);

        ListPopup popup = createPopup(step, renderer, popupConfig);

        CompletableFuture<@Nullable T> result = new CompletableFuture<>();
        CollaborationToolsPopupUtil.<@Nullable T>completeOnClose(popup, result, chosen::get);
        configure.accept(new Created<>(popup, step));
        return new ChooserPopup<@Nullable T>(popup, result);
    }

    @RequiredUIAccess
    private static <T> ChooserPopup<List<T>> createMultipleChooser(MultiChooserListModel<T> chooserModel,
                                                                  Function<T, PopupItemPresentation> presenter,
                                                                  PopupConfig popupConfig,
                                                                  Consumer<Created<T>> configure) {
        MutableFlatDataModel<Object> model = FlatDataModel.of(new ArrayList<Object>(chooserModel.getItems()));
        ChooserPopupStep<T> step = new ChooserPopupStep<>(popupConfig.title(), model, filterByNamesFromPresentation(presenter),
            chooserModel, value -> {
        });

        TextItemRenderer<T> renderer = SimpleSelectablePopupItemRenderer.create(item -> {
            PopupItemPresentation presentation = presenter.apply(item);
            return new SelectablePopupItemPresentation.Simple(
                presentation.getShortText(),
                presentation.getIcon(),
                presentation.getFullText(),
                chooserModel.isChosen(item)
            );
        });

        ListPopup popup = createPopup(step, renderer, popupConfig);
        // non-empty list returns pref size without considering "visibleRowCount"
        popup.setMinimumWidth(MULTIPLE_CHOOSER_MIN_WIDTH);

        CompletableFuture<List<T>> result = new CompletableFuture<>();
        CollaborationToolsPopupUtil.completeOnClose(popup, result, chooserModel::getChosenItems);
        configure.accept(new Created<>(popup, step));
        return new ChooserPopup<>(popup, result);
    }

    @RequiredUIAccess
    private static <T> ListPopup createPopup(ChooserPopupStep<T> step, TextItemRenderer<T> renderer, PopupConfig popupConfig) {
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.setRenderer(statusAwareRenderer(renderer));
        popup.setResizable(popupConfig.isResizable());
        return popup;
    }

    @RequiredUIAccess
    private static <T> void loadAccumulated(Created<T> created,
                                            Function<Consumer<List<T>>, CompletableFuture<?>> loader,
                                            PopupConfig popupConfig,
                                            Function<List<T>, List<T>> merge) {
        UIAccess uiAccess = UIAccess.current();
        AtomicReference<List<T>> shown = new AtomicReference<>(List.of());
        ChooserStatusRow loading = new ChooserStatusRow(ReviewLocalize.reviewPopupDataLoading(), false);
        created.step.setItems(List.of(), loading);

        CompletableFuture<?> loadingFuture = loader.apply(items -> uiAccess.give(() -> {
            if (!created.popup.isDisposed()) {
                shown.set(merge.apply(items));
                created.step.setItems(shown.get(), loading);
            }
        }));
        loadingFuture.whenComplete((ignored, error) -> uiAccess.give(() -> {
            if (created.popup.isDisposed()) {
                return;
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            created.step.setItems(shown.get(), cause == null ? null : errorRow(cause, popupConfig));
        }));
        Disposer.register(created.popup, () -> loadingFuture.cancel(false));
    }

    private static <T> @Nullable ChooserStatusRow statusOf(IncrementallyComputedValue<T> state, PopupConfig popupConfig) {
        Exception exception = state.getExceptionOrNull();
        if (exception != null) {
            return errorRow(exception, popupConfig);
        }
        return state.isLoading() ? new ChooserStatusRow(ReviewLocalize.reviewPopupDataLoading(), false) : null;
    }

    private static ChooserStatusRow errorRow(Throwable e, PopupConfig popupConfig) {
        TextErrorStatusPresenter<Throwable> errorPresenter = popupConfig.errorPresenter();
        if (errorPresenter != null) {
            return new ChooserStatusRow(errorPresenter.getErrorTitle(e), true);
        }
        String errorMessage = e.getLocalizedMessage();
        return new ChooserStatusRow(errorMessage != null ? LocalizeValue.of(errorMessage) : ReviewLocalize.popupDataLoadingError(), true);
    }

    @SuppressWarnings("unchecked")
    private static <T> TextItemRenderer<Object> statusAwareRenderer(TextItemRenderer<T> renderer) {
        return (presentation, item) -> {
            Object value = item.getValue();
            if (value instanceof ChooserStatusRow status) {
                presentation.append(status.text(), status.error() ? TextAttribute.ERROR : TextAttribute.GRAYED);
                return;
            }
            renderer.render(presentation, (RenderItem<T>) item);
        };
    }

    private static <T> Function<T, String> filterByNamesFromPresentation(Function<T, PopupItemPresentation> presenter) {
        return item -> {
            PopupItemPresentation presentation = presenter.apply(item);
            String fullText = presentation.getFullText();
            return fullText != null ? presentation.getShortText() + " " + fullText : presentation.getShortText();
        };
    }
}
