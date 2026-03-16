// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.application.progress.ProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface WeightedSearchEverywhereContributor<I> extends SearchEverywhereContributor<I> {
    void fetchWeightedElements(
        String pattern,
        ProgressIndicator progressIndicator,
        Predicate<? super FoundItemDescriptor<I>> predicate
    );

    @Override
    default void fetchElements(
        String pattern,
        ProgressIndicator progressIndicator,
        Predicate<? super I> predicate
    ) {
        fetchWeightedElements(pattern, progressIndicator, descriptor -> predicate.test(descriptor.getItem()));
    }

    
    default ContributorSearchResult<? super FoundItemDescriptor<I>> searchWeightedElements(
        String pattern,
        ProgressIndicator progressIndicator,
        int elementsLimit
    ) {
        ContributorSearchResult.Builder<? super FoundItemDescriptor<I>> builder = ContributorSearchResult.builder();
        fetchWeightedElements(pattern, progressIndicator, descriptor -> {
            if (elementsLimit < 0 || builder.itemsCount() < elementsLimit) {
                builder.addItem(descriptor);
                return true;
            }
            else {
                builder.setHasMore(true);
                return false;
            }
        });

        return builder.build();
    }

    
    default List<? super FoundItemDescriptor<I>> searchWeightedElements(
        String pattern,
        ProgressIndicator progressIndicator
    ) {
        List<? super FoundItemDescriptor<I>> res = new ArrayList<>();
        fetchWeightedElements(pattern, progressIndicator, res::add);
        return res;
    }
}
