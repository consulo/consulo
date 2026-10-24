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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.details.CommitPresentation;
import consulo.review.util.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitsComponentFactory;
import consulo.versionControlSystem.review.details.model.CodeReviewChangesViewModel;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class CodeReviewDetailsCommitsComponentFactoryImpl implements CodeReviewDetailsCommitsComponentFactory {
    private static final Space COMPONENTS_GAP = Space.SMALL;
    static final Space VERT_PADDING = Space.MEDIUM;

    private static final Object ALL_COMMITS = new Object();

    @RequiredUIAccess
    @Override
    public <T> Component create(Disposable parentDisposable,
                                CodeReviewChangesViewModel<T> changesVm,
                                Function<T, CommitPresentation> commitPresentation) {
        Label commitsPopupTitle = Label.create(LocalizeValue.empty());
        commitsPopupTitle.addStyle(LabelStyle.FONT_BOLD);
        ReviewUIUtil.bind(parentDisposable, changesVm.getReviewCommits(), commits ->
            commitsPopupTitle.setText(ReviewLocalize.reviewDetailsCommitsTitleText(commits.size())));

        Hyperlink commitsPopup = createCommitChooserActionLink(parentDisposable, changesVm, commitPresentation);

        Button nextCommitIcon = createIconButton(PlatformIconGroup.actionsNextoccurence(), ReviewLocalize.reviewDetailsCommitsNext());
        nextCommitIcon.addClickListener(event -> changesVm.selectNextCommit());

        Button previousCommitIcon = createIconButton(PlatformIconGroup.actionsPreviousoccurence(), ReviewLocalize.reviewDetailsCommitsPrevious());
        previousCommitIcon.addClickListener(event -> changesVm.selectPreviousCommit());

        ReviewUIUtil.bind(parentDisposable, changesVm.getSelectedCommitIndex(), changesVm.getReviewCommits(), (selectedCommitIndex, commits) -> {
            boolean visible = commits.size() > 1 && selectedCommitIndex >= 0;
            nextCommitIcon.setVisible(visible);
            previousCommitIcon.setVisible(visible);
            nextCommitIcon.setEnabled(selectedCommitIndex != commits.size() - 1);
            previousCommitIcon.setEnabled(selectedCommitIndex != 0);
        });

        HorizontalLayout panel = HorizontalLayout.create(COMPONENTS_GAP);
        panel.paddingBuilder().verticalSet(VERT_PADDING).apply();
        panel.add(commitsPopupTitle);
        panel.add(commitsPopup);
        panel.add(nextCommitIcon);
        panel.add(previousCommitIcon);
        return panel;
    }

    @RequiredUIAccess
    private static Button createIconButton(Image icon, LocalizeValue tooltip) {
        Button button = Button.create(LocalizeValue.empty());
        button.setIcon(icon);
        button.addStyle(ButtonStyle.INPLACE);
        button.setToolTipText(tooltip);
        button.setVisible(false);
        return button;
    }

    @RequiredUIAccess
    private static <T> Hyperlink createCommitChooserActionLink(Disposable parentDisposable,
                                                               CodeReviewChangesViewModel<T> changesVm,
                                                               Function<T, CommitPresentation> commitPresentation) {
        Hyperlink link = Hyperlink.create(LocalizeValue.empty());
        link.setIcon(VersionControlSystemIconGroup.commit());
        link.addHyperlinkListener(event -> showCommitPopup(link, changesVm, commitPresentation));

        ReviewUIUtil.bind(parentDisposable, changesVm.getSelectedCommit(), changesVm.getReviewCommits(), (selectedCommit, commits) -> {
            if (selectedCommit != null && !commits.isEmpty()) {
                link.setText(LocalizeValue.of(changesVm.commitHash(selectedCommit)));
            }
            else {
                link.setText(ReviewLocalize.reviewDetailsCommitsPopupText(commits.size()));
            }
            link.setEnabled(commits.size() > 1);
        });
        return link;
    }

    @RequiredUIAccess
    private static <T> void showCommitPopup(Component anchor,
                                            CodeReviewChangesViewModel<T> changesVm,
                                            Function<T, CommitPresentation> commitPresentation) {
        List<T> commits = changesVm.getReviewCommits().get();
        int commitsCount = commits.size();
        T selectedCommit = changesVm.getSelectedCommit().get();

        List<Object> popupItems = new ArrayList<>(commitsCount + 1);
        popupItems.add(ALL_COMMITS);
        popupItems.addAll(commits);

        BaseListPopupStep<Object> step = new BaseListPopupStep<>(null, popupItems) {
            @Override
            @SuppressWarnings("unchecked")
            public String getTextFor(Object value) {
                if (value == ALL_COMMITS) {
                    return ReviewLocalize.reviewDetailsCommitsPopupAll(commitsCount).get();
                }
                return plainText(commitPresentation.apply((T) value).titleHtml());
            }

            @Override
            public @Nullable ListSeparator getSeparatorAbove(Object value) {
                return commitsCount > 0 && value == commits.get(0) ? new ListSeparator() : null;
            }

            @Override
            public int getDefaultOptionIndex() {
                return selectedCommit == null ? 0 : commits.indexOf(selectedCommit) + 1;
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public @Nullable PopupStep<?> onChosen(Object value, boolean finalChoice) {
                changesVm.selectCommit(value == ALL_COMMITS ? -1 : commits.indexOf(value));
                return FINAL_CHOICE;
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.setRenderer((presentation, item) -> {
            Object value = item.getValue();
            boolean selected = value == ALL_COMMITS ? selectedCommit == null : Objects.equals(value, selectedCommit);
            presentation.withIcon(selected ? PlatformIconGroup.actionsChecked_selected() : Image.empty(Image.DEFAULT_ICON_SIZE));
            if (value == ALL_COMMITS || value == null) {
                presentation.append(ReviewLocalize.reviewDetailsCommitsPopupAll(commitsCount));
                return;
            }
            @SuppressWarnings("unchecked")
            CommitPresentation commit = commitPresentation.apply((T) value);
            presentation.append(LocalizeValue.of(plainText(commit.titleHtml())), TextAttribute.REGULAR);
            presentation.withSuffix(LocalizeValue.of(commit.author() + ", " + DateFormatUtil.formatPrettyDateTime(commit.committedDate())), null);
        });
        popup.showBy(anchor, null, PopupPosition.BOTTOM);
    }

    static String plainText(String html) {
        return StringUtil.stripHtml(html, false);
    }
}
