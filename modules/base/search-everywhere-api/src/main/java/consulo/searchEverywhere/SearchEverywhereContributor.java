// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Konstantin Bulenkov
 */
public interface SearchEverywhereContributor<Item> extends PossiblyDumbAware {
    
    String getSearchProviderId();

    
    String getGroupName();

    
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
    default int getElementPriority(Item element, String searchPattern) {
        return 0;
    }

    
    default List<SearchEverywhereCommandInfo> getSupportedCommands() {
        return Collections.emptyList();
    }

    default @Nullable String getAdvertisement() {
        return null;
    }

    
    default List<AnAction> getActions(Runnable onChanged) {
        return Collections.emptyList();
    }

    void fetchElements(String pattern, ProgressIndicator progressIndicator, Predicate<? super Item> predicate);

    
    default ContributorSearchResult<Item> search(String pattern, ProgressIndicator progressIndicator, int elementsLimit) {
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

    
    default List<Item> search(String pattern, ProgressIndicator progressIndicator) {
        List<Item> res = new ArrayList<>();
        fetchElements(pattern, progressIndicator, res::add);
        return res;
    }

    boolean processSelectedItem(Item selected, int modifiers, String searchText);

    
    ListCellRenderer<? super Item> getElementsRenderer();

    @Nullable Object getDataForItem(Item element, Key dataId);

    
    default String filterControlSymbols(String pattern) {
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
