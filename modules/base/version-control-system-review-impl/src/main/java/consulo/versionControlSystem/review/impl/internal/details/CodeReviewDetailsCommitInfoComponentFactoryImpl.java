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
package consulo.versionControlSystem.review.impl.internal.details;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.DateFormatUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.details.CommitPresentation;
import consulo.review.util.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitInfoComponentFactory;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@Singleton
@ServiceImpl
public class CodeReviewDetailsCommitInfoComponentFactoryImpl implements CodeReviewDetailsCommitInfoComponentFactory {
    private static final Space GAP = Space.LARGE;

    @RequiredUIAccess
    @Override
    public <T> Component create(Disposable parentDisposable,
                                ObservableValue<@Nullable T> commit,
                                Function<T, CommitPresentation> commitPresentation) {
        MutableObservableValue<Boolean> withDetailedInfo = MutableObservableValue.of(false);

        HtmlLabel title = HtmlLabel.create(LocalizeValue.empty());
        HtmlLabel description = HtmlLabel.create(LocalizeValue.empty());
        Label info = Label.create(LocalizeValue.empty());
        info.setForegroundColor(ComponentColors.DISABLED_TEXT);

        Hyperlink detailsAction = Hyperlink.create(ReviewLocalize.reviewDetailsCommitsDetailsShow());
        detailsAction.addHyperlinkListener(event -> withDetailedInfo.set(!withDetailedInfo.get()));

        DockLayout topPanel = DockLayout.create();
        topPanel.center(title);
        topPanel.right(detailsAction);

        VerticalLayout panel = VerticalLayout.create(GAP);
        panel.add(topPanel);
        panel.add(description);
        panel.add(info);

        ReviewUIUtil.bind(parentDisposable, commit, value -> panel.setVisible(value != null));

        ReviewUIUtil.bind(parentDisposable, withDetailedInfo, isDetailed -> {
            description.setVisible(isDetailed);
            info.setVisible(isDetailed);
            detailsAction.setText(isDetailed
                ? ReviewLocalize.reviewDetailsCommitsDetailsHide()
                : ReviewLocalize.reviewDetailsCommitsDetailsShow());
        });

        ReviewUIUtil.bind(parentDisposable, commit, value -> {
            if (value == null) {
                return;
            }
            CommitPresentation presentation = commitPresentation.apply(value);
            title.setText(LocalizeValue.of(presentation.titleHtml()));
            description.setText(LocalizeValue.of(presentation.descriptionHtml()));
            info.setText(LocalizeValue.of(presentation.authorAndDate(DateFormatUtil::formatPrettyDateTime)));
        });
        return panel;
    }
}
