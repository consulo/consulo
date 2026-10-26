// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.details;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.review.details.ReviewDetailsUIUtil;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.model.CodeReviewDetailsViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.review.impl.internal.list.ReviewListCellRenderer;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

/**
 * The title of a review details tab: the review title with its number linking to the review, and the state tag.
 */
public final class CodeReviewDetailsTitleComponentFactory {
    private CodeReviewDetailsTitleComponentFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable, CodeReviewDetailsViewModel detailsVm) {
        Label titleLabel = Label.create();
        titleLabel.addStyle(LabelStyle.FONT_X_LARGE);
        titleLabel.addStyle(LabelStyle.FONT_BOLD);
        ReviewUIUtil.bind(parentDisposable, detailsVm.getTitle(), title -> titleLabel.setText(LocalizeValue.of(title)));

        Hyperlink numberLink = Hyperlink.create(LocalizeValue.of(detailsVm.getNumber()),
            event -> Platform.current().openInBrowser(detailsVm.getUrl()));
        numberLink.setToolTipText(LocalizeValue.of(detailsVm.getUrl()));

        Label stateLabel = ReviewListCellRenderer.createTagLabel();
        ReviewUIUtil.bind(parentDisposable, detailsVm.getReviewRequestState(), state -> {
            stateLabel.setText(ReviewDetailsUIUtil.getRequestStateText(state));
            stateLabel.setVisible(state != ReviewRequestState.OPENED);
        });

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.center(HorizontalLayout.create(Space.SMALL).add(titleLabel).add(numberLink));
        panel.right(VerticalLayout.create(Space.NONE).add(stateLabel));
        return panel;
    }
}
