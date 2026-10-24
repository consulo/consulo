// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.details;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.data.ReviewState;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.VerticalLayout;

public final class ReviewDetailsUIUtil {
    private static final Space TITLE_PANEL_GAP = Space.LARGE;

    private ReviewDetailsUIUtil() {
    }

    @RequiredUIAccess
    public static Component createTitlePanel(Component title, Component timelineLink) {
        return VerticalLayout.create(TITLE_PANEL_GAP).add(title).add(timelineLink);
    }

    public static LocalizeValue getRequestStateText(ReviewRequestState state) {
        return switch (state) {
            case OPENED -> ReviewLocalize.reviewDetailsReviewStateOpen();
            case CLOSED -> ReviewLocalize.reviewDetailsReviewStateClosed();
            case MERGED -> ReviewLocalize.reviewDetailsReviewStateMerged();
            case DRAFT -> ReviewLocalize.reviewDetailsReviewStateDraft();
        };
    }

    public static Image getReviewStateIcon(ReviewState reviewState) {
        return switch (reviewState) {
            case ACCEPTED -> PlatformIconGroup.statusSuccess();
            case WAIT_FOR_UPDATES -> PlatformIconGroup.generalError();
            case NEED_REVIEW -> PlatformIconGroup.generalWarning();
        };
    }

    public static LocalizeValue getReviewStateText(ReviewState reviewState, String reviewer) {
        return switch (reviewState) {
            case ACCEPTED -> ReviewLocalize.reviewDetailsStatusReviewerApproved(reviewer);
            case WAIT_FOR_UPDATES -> ReviewLocalize.reviewDetailsStatusReviewerWaitForUpdates(reviewer);
            case NEED_REVIEW -> ReviewLocalize.reviewDetailsStatusReviewerNeedReview(reviewer);
        };
    }

    /**
     * The outline of the reviewer's avatar telling the state of the review.
     */
    public static AvatarStyle getReviewStateAvatarStyle(ReviewState reviewState) {
        return switch (reviewState) {
            case ACCEPTED -> AvatarStyle.OUTLINE_SUCCESS;
            case WAIT_FOR_UPDATES -> AvatarStyle.OUTLINE_WARNING;
            case NEED_REVIEW -> AvatarStyle.OUTLINE_NEUTRAL;
        };
    }
}
