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
import consulo.localize.LocalizeValue;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.list.search.ChooserPopup;
import consulo.review.list.search.ChooserPopupUtil;
import consulo.review.list.search.DropDownComponentFactory;
import consulo.review.list.search.PopupConfig;
import consulo.review.list.search.ReviewListSearchFiltersDropDownComponentFactory;
import consulo.review.list.search.ReviewListSearchPanelFactory;
import consulo.review.util.popup.PopupItemPresentation;
import consulo.ui.ex.action.AnAction;

import java.util.List;
import java.util.Locale;

/**
 * Author and state are single choices from a fixed list; labels are several, loaded while the chooser is open.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewSearchPanelFactory
    extends ReviewListSearchPanelFactory<SandReviewSearchValue, SandReviewQuickFilter, SandReviewSearchPanelViewModel> {

    public SandReviewSearchPanelFactory(SandReviewSearchPanelViewModel vm) {
        super(vm);
    }

    @Override
    protected LocalizeValue getShortText(SandReviewSearchValue searchValue) {
        return LocalizeValue.of(searchValue.getShortText());
    }

    @Override
    protected List<AnAction> createFilters(Disposable viewDisposable) {
        List<SandReviewUser> users = List.of(SandReviewService.CURRENT_USER, SandReviewService.REVIEWER);

        AnAction authorFilter = new DropDownComponentFactory<>(myVm.getAuthorFilterState()).create(
            LocalizeValue.localizeTODO("Author"),
            users.stream().map(SandReviewUser::getUsername).toList(),
            () -> {
            },
            username -> username,
            username -> {
                SandReviewUser user = users.stream().filter(it -> it.getUsername().equals(username)).findFirst().orElseThrow();
                return new PopupItemPresentation.Simple(user.getUsername(), user.getAvatarIcon(), user.getFullName());
            }
        );

        AnAction stateFilter = new DropDownComponentFactory<>(myVm.getStateFilterState()).create(
            LocalizeValue.localizeTODO("State"),
            List.of(ReviewRequestState.values()),
            () -> {
            },
            state -> state.name().toLowerCase(Locale.ROOT)
        );

        AnAction labelsFilter = ReviewListSearchFiltersDropDownComponentFactory.create(
            myVm.getLabelsFilterState(),
            LocalizeValue.localizeTODO("Label"),
            labels -> String.join(", ", labels),
            () -> {
                List<String> current = myVm.getLabelsFilterState().get();
                ChooserPopup<List<String>> chooser = ChooserPopupUtil.createMultipleChooserPopupWithIncrementalLoading(
                    current == null ? List.of() : current,
                    myVm.loadLabels(),
                    PopupItemPresentation.Simple::new,
                    PopupConfig.DEFAULT.withTitle(LocalizeValue.localizeTODO("Labels"))
                );
                chooser.result().thenAccept(labels -> myVm.getLabelsFilterState().set(labels.isEmpty() ? null : labels));
                return chooser.popup();
            },
            () -> myVm.getLabelsFilterState().set(null)
        );

        return List.of(authorFilter, stateFilter, labelsFilter);
    }

    @Override
    protected LocalizeValue getQuickFilterTitle(SandReviewQuickFilter quickFilter) {
        return quickFilter.getTitle();
    }
}
