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
import consulo.review.details.data.ReviewRequestState;
import consulo.review.list.search.ReviewListQuickFilter;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public enum SandReviewQuickFilter implements ReviewListQuickFilter<SandReviewSearchValue> {
    OPEN(LocalizeValue.localizeTODO("Open"), SandReviewSearchValue.DEFAULT),
    MINE(LocalizeValue.localizeTODO("Created by Me"),
        new SandReviewSearchValue(null, ReviewRequestState.OPENED, SandReviewService.CURRENT_USER.getUsername(), List.of())),
    DRAFTS(LocalizeValue.localizeTODO("Drafts"), new SandReviewSearchValue(null, ReviewRequestState.DRAFT, null, List.of())),
    MERGED(LocalizeValue.localizeTODO("Merged"), new SandReviewSearchValue(null, ReviewRequestState.MERGED, null, List.of()));

    private final LocalizeValue myTitle;
    private final SandReviewSearchValue myFilter;

    SandReviewQuickFilter(LocalizeValue title, SandReviewSearchValue filter) {
        myTitle = title;
        myFilter = filter;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    @Override
    public SandReviewSearchValue getFilter() {
        return myFilter;
    }
}
