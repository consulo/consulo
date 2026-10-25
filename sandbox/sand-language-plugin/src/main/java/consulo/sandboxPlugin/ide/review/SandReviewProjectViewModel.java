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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.toolwindow.ReviewToolwindowProjectViewModel;
import consulo.review.toolwindow.ReviewToolwindowTabs;
import consulo.review.toolwindow.ReviewToolwindowTabsStateHolder;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewProjectViewModel implements ReviewToolwindowProjectViewModel<SandReviewTab, SandReviewTabViewModel> {
    private final SandReviewListViewModel myListVm;
    private final SandReviewSearchPanelViewModel mySearchVm;
    private final ReviewToolwindowTabsStateHolder<SandReviewTab, SandReviewTabViewModel> myTabs = new ReviewToolwindowTabsStateHolder<>();

    public SandReviewProjectViewModel(Disposable parentDisposable,
                                      ScheduledExecutorService scheduler,
                                      Supplier<List<SandReviewViewModel>> loader) {
        mySearchVm = new SandReviewSearchPanelViewModel(parentDisposable, scheduler);
        myListVm = new SandReviewListViewModel(scheduler, () -> {
            SandReviewSearchValue search = mySearchVm.getSearchState().get();
            return loader.get().stream().filter(search::matches).toList();
        }, this::openReview);
        Runnable unsubscribe = mySearchVm.getSearchState().addListener(search -> myListVm.refresh());
        Disposer.register(parentDisposable, unsubscribe::run);
    }

    public SandReviewSearchPanelViewModel getSearchVm() {
        return mySearchVm;
    }

    @Override
    public LocalizeValue getProjectName() {
        return LocalizeValue.of("sand/review-demo");
    }

    @Override
    public SandReviewListViewModel getListVm() {
        return myListVm;
    }

    @Override
    public ObservableValue<ReviewToolwindowTabs<SandReviewTab, SandReviewTabViewModel>> getTabs() {
        return myTabs.getTabs();
    }

    public void openReview(SandReviewViewModel review) {
        myTabs.showTab(new SandReviewTab(review.getNumber()), SandReviewTabViewModel.class, tab -> new SandReviewTabViewModel(review));
    }

    @Override
    public void selectTab(@Nullable SandReviewTab tab) {
        myTabs.select(tab != null && myTabs.getTabs().get().tabs().containsKey(tab) ? tab : null);
    }

    @Override
    public void closeTab(SandReviewTab tab) {
        myTabs.close(tab);
    }
}
