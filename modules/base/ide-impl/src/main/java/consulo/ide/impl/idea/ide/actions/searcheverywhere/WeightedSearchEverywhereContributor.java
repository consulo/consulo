// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.progress.ProgressIndicator;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface WeightedSearchEverywhereContributor<I> extends consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereContributor<I> {
    void fetchWeightedElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super FoundItemDescriptor<I>> predicate
    );

    @Override
    default void fetchElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super I> predicate
    ) {
        fetchWeightedElements(pattern, progressIndicator, descriptor -> predicate.test(descriptor.getItem()));
    }

    @Nonnull
    default ContributorSearchResult<? super FoundItemDescriptor<I>> searchWeightedElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
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

    @Nonnull
    default List<? super FoundItemDescriptor<I>> searchWeightedElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator
    ) {
        List<? super FoundItemDescriptor<I>> res = new ArrayList<>();
        fetchWeightedElements(pattern, progressIndicator, res::add);
        return res;
    }
}
