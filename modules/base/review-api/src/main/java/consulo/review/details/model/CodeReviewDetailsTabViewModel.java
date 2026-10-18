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
package consulo.review.details.model;

import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.toolwindow.ReviewTabViewModel;
import org.jspecify.annotations.Nullable;

/**
 * A review tool window tab showing one review.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewDetailsTabViewModel extends ReviewTabViewModel {
    CodeReviewDetailsViewModel getDetailsVm();

    @Nullable
    default CodeReviewStatusViewModel getStatusVm() {
        return null;
    }

    @Nullable
    default CodeReviewTimelineViewModel getTimelineVm() {
        return null;
    }
}
