// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Konstantin Bulenkov
 */
public interface SearchEverywhereContributor<Item> extends PossiblyDumbAware {
    @Nonnull
    String getSearchProviderId();

    @Nonnull
    String getGroupName();

    @Nonnull
    default String getFullGroupName() {
        return getGroupName();
    }

    int getSortWeight();

    boolean showInFindResults();

    default boolean isShownInSeparateTab() {
        return false;
    }

    /**
     * @deprecated method is left for backward compatibility only. If you want to consider elements weight in your search contributor
     * please use {@link WeightedSearchEverywhereContributor#fetchWeightedElements(String, ProgressIndicator, Predicate)} method for fetching
     * this elements
     */
    @Deprecated
    default int getElementPriority(@Nonnull Item element, @Nonnull String searchPattern) {
        return 0;
    }

    @Nonnull
    default List<SearchEverywhereCommandInfo> getSupportedCommands() {
        return Collections.emptyList();
    }

    @Nullable
    default String getAdvertisement() {
        return null;
    }

    @Nonnull
    default List<AnAction> getActions(@Nonnull Runnable onChanged) {
        return Collections.emptyList();
    }

    void fetchElements(@Nonnull String pattern, @Nonnull ProgressIndicator progressIndicator, @Nonnull Predicate<? super Item> predicate);

    @Nonnull
    default ContributorSearchResult<Item> search(@Nonnull String pattern, @Nonnull ProgressIndicator progressIndicator, int elementsLimit) {
        ContributorSearchResult.Builder<Item> builder = ContributorSearchResult.builder();
        fetchElements(pattern, progressIndicator, element -> {
            if (elementsLimit < 0 || builder.itemsCount() < elementsLimit) {
                builder.addItem(element);
                return true;
            }
            else {
                builder.setHasMore(true);
                return false;
            }
        });

        return builder.build();
    }

    @Nonnull
    default List<Item> search(@Nonnull String pattern, @Nonnull ProgressIndicator progressIndicator) {
        List<Item> res = new ArrayList<>();
        fetchElements(pattern, progressIndicator, res::add);
        return res;
    }

    boolean processSelectedItem(@Nonnull Item selected, int modifiers, @Nonnull String searchText);

    @Nonnull
    ListCellRenderer<? super Item> getElementsRenderer();

    @Nullable
    Object getDataForItem(@Nonnull Item element, @Nonnull Key dataId);

    @Nonnull
    default String filterControlSymbols(@Nonnull String pattern) {
        return pattern;
    }

    default boolean isMultiSelectionSupported() {
        return false;
    }

    @Override
    default boolean isDumbAware() {
        return true;
    }

    default boolean isEmptyPatternSupported() {
        return false;
    }
}
