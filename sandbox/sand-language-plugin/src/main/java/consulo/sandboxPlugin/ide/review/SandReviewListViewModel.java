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
package consulo.sandboxPlugin.ide.review;

import consulo.review.list.CodeReviewListItemsViewModel;
import consulo.review.list.ReviewListItemPresentation;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewListViewModel implements CodeReviewListItemsViewModel<SandReviewViewModel> {
    private final ScheduledExecutorService myScheduler;
    private final Supplier<List<SandReviewViewModel>> myLoader;
    private final Consumer<SandReviewViewModel> myOpener;
    private final MutableObservableValue<ComputedResult<List<SandReviewViewModel>>> myReviews =
        MutableObservableValue.of(ComputedResult.loading());

    public SandReviewListViewModel(ScheduledExecutorService scheduler,
                                   Supplier<List<SandReviewViewModel>> loader,
                                   Consumer<SandReviewViewModel> opener) {
        myScheduler = scheduler;
        myLoader = loader;
        myOpener = opener;
        refresh();
    }

    @Override
    public ObservableValue<ComputedResult<List<SandReviewViewModel>>> getItems() {
        return myReviews;
    }

    @Override
    public ReviewListItemPresentation getPresentation(SandReviewViewModel item) {
        return item.toPresentation();
    }

    @Override
    public void open(SandReviewViewModel item) {
        item.setSeen(null);
        myOpener.accept(item);
    }

    @Override
    public void refresh() {
        ComputedResult<List<SandReviewViewModel>> current = myReviews.get();
        if (!current.isSuccess()) {
            myReviews.set(ComputedResult.loading());
        }
        myScheduler.schedule(() -> myReviews.set(ComputedResult.success(List.copyOf(myLoader.get()))), 700, TimeUnit.MILLISECONDS);
    }
}
