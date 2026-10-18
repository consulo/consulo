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

import consulo.localize.LocalizeValue;
import consulo.review.toolwindow.ReviewToolwindowProjectViewModel;
import consulo.review.toolwindow.ReviewToolwindowTabs;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewProjectViewModel implements ReviewToolwindowProjectViewModel<SandReviewTab, SandReviewTabViewModel> {
    private final SandReviewListViewModel myListVm;
    private final MutableObservableValue<ReviewToolwindowTabs<SandReviewTab, SandReviewTabViewModel>> myTabs =
        MutableObservableValue.of(new ReviewToolwindowTabs<>(Map.of(), null));

    public SandReviewProjectViewModel(SandReviewListViewModel listVm) {
        myListVm = listVm;
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
        return myTabs;
    }

    public void openReview(SandReviewViewModel review) {
        SandReviewTab tab = new SandReviewTab(review.getNumber());
        myTabs.update(tabs -> {
            Map<SandReviewTab, SandReviewTabViewModel> result = new LinkedHashMap<>(tabs.tabs());
            result.putIfAbsent(tab, new SandReviewTabViewModel(review));
            return new ReviewToolwindowTabs<>(result, tab);
        });
    }

    @Override
    public void selectTab(@Nullable SandReviewTab tab) {
        myTabs.update(tabs -> new ReviewToolwindowTabs<>(tabs.tabs(), tab != null && tabs.tabs().containsKey(tab) ? tab : null));
    }

    @Override
    public void closeTab(SandReviewTab tab) {
        myTabs.update(tabs -> {
            Map<SandReviewTab, SandReviewTabViewModel> result = new LinkedHashMap<>(tabs.tabs());
            result.remove(tab);
            return new ReviewToolwindowTabs<>(result, tab.equals(tabs.selectedTab()) ? null : tabs.selectedTab());
        });
    }
}
