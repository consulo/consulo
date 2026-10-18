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
import consulo.review.details.model.CodeReviewDetailsViewModel;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * The description of a review details tab, cut to its first lines, and the link to the whole timeline.
 */
public final class CodeReviewDetailsDescriptionComponentFactory {
    private static final int VISIBLE_DESCRIPTION_LINES = 2;

    private CodeReviewDetailsDescriptionComponentFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable,
                                   CodeReviewDetailsViewModel detailsVm,
                                   @Nullable @RequiredUIAccess Runnable showTimelineAction) {
        VerticalLayout panel = VerticalLayout.create(Space.SMALL);

        ObservableValue<String> description = detailsVm.getDescription();
        if (description != null) {
            Label descriptionLabel = Label.create();
            ReviewUIUtil.bind(parentDisposable, description, text -> {
                descriptionLabel.setText(LocalizeValue.of(firstLines(text)));
                descriptionLabel.setVisible(!text.isBlank());
            });
            panel.add(descriptionLabel);
        }

        if (showTimelineAction != null) {
            panel.add(Hyperlink.create(ReviewLocalize.reviewDetailsViewTimelineAction(), event -> showTimelineAction.run()));
        }
        return panel;
    }

    private static String firstLines(String text) {
        String[] lines = text.strip().split("\\R", VISIBLE_DESCRIPTION_LINES + 1);
        if (lines.length <= VISIBLE_DESCRIPTION_LINES) {
            return String.join("\n", lines);
        }
        return String.join("\n", Arrays.copyOf(lines, VISIBLE_DESCRIPTION_LINES)) + "…";
    }
}
