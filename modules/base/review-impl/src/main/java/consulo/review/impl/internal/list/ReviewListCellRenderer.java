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
package consulo.review.impl.internal.list;

import consulo.application.util.DateFormatUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.list.CommentsCounter;
import consulo.review.list.NamedCollection;
import consulo.review.list.ReviewListItemPresentation;
import consulo.review.list.ReviewListItemStatus;
import consulo.review.list.TagPresentation;
import consulo.review.list.UserPresentation;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.RenderItem;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One row of the review list: the title line with its status markers on the right, and the info line under it.
 */
public final class ReviewListCellRenderer<T> {
    private static final int MAX_PARTICIPANT_ICONS = 2;
    private static final int AVATAR_SIZE = 20;
    private static final Space GAP = Space.MEDIUM;

    private static final Key<ReviewListCellRenderer<?>> RENDERER_KEY = Key.create("consulo.review.list.cell.renderer");

    public static <T> ComponentItemRenderer<T> create(Function<T, ReviewListItemPresentation> presenter) {
        return ComponentItemRenderer.reusable(
            () -> new ReviewListCellRenderer<>(presenter).myRoot,
            (root, item) -> {
                @SuppressWarnings("unchecked")
                ReviewListCellRenderer<T> renderer = (ReviewListCellRenderer<T>) Objects.requireNonNull(root.getUserData(RENDERER_KEY));
                renderer.bind(item);
            }
        );
    }

    private final Function<T, ReviewListItemPresentation> myPresenter;

    private final DockLayout myRoot;
    private final Label myUnseen;
    private final Label myTitle;
    private final Label myTags;
    private final Label myInfo;
    private final Label myStateLabel;
    private final Label myNonMergeable;
    private final Label myBuildStatus;
    private final HorizontalLayout myUserGroup1;
    private final HorizontalLayout myUserGroup2;
    private final Label myComments;

    @RequiredUIAccess
    private ReviewListCellRenderer(Function<T, ReviewListItemPresentation> presenter) {
        myPresenter = presenter;

        myUnseen = Label.create();
        myUnseen.setImage(ReviewIconGroup.reviewUnreaddot());
        myUnseen.paddingBuilder().leftSet(Space.X_SMALL).apply();

        myTitle = Label.create();

        myTags = Label.create();
        myTags.setImage(PlatformIconGroup.nodesTag());

        myInfo = Label.create();
        myInfo.addStyle(LabelStyle.FONT_SMALL);

        myStateLabel = createTagLabel();

        myNonMergeable = Label.create();
        myBuildStatus = Label.create();
        myUserGroup1 = HorizontalLayout.create(Space.NONE);
        myUserGroup2 = HorizontalLayout.create(Space.NONE);

        myComments = Label.create();
        myComments.setImage(ReviewIconGroup.comment());
        myComments.paddingBuilder().rightSet(Space.X_SMALL).apply();

        HorizontalLayout left = HorizontalLayout.create(GAP).add(myUnseen).add(myTitle).add(myTags);
        HorizontalLayout right = HorizontalLayout.create(GAP)
            .add(myStateLabel)
            .add(myNonMergeable)
            .add(myBuildStatus)
            .add(myUserGroup1)
            .add(myUserGroup2)
            .add(myComments);

        DockLayout firstLine = DockLayout.create();
        firstLine.center(left);
        firstLine.right(right);

        myRoot = DockLayout.create();
        myRoot.center(firstLine);
        myRoot.bottom(myInfo);
        myRoot.paddingBuilder().topSet(Space.SMALL).bottomSet(Space.SMALL).horizontalSet(Space.X_LARGE).apply();
        myRoot.putUserData(RENDERER_KEY, this);
    }

    @RequiredUIAccess
    public static Label createTagLabel() {
        Label label = Label.create();
        label.addStyle(LabelStyle.FONT_SMALL);
        label.setForegroundColor(CodeReviewColorUtil.REVIEW_STATE_FOREGROUND);
        label.setBackgroundColor(CodeReviewColorUtil.REVIEW_STATE_BACKGROUND);
        label.paddingBuilder().horizontalSet(Space.SMALL).apply();
        return label;
    }

    @RequiredUIAccess
    private void bind(RenderItem<T> item) {
        T value = item.getValue();
        if (value == null) {
            return;
        }
        ReviewListItemPresentation presentation = myPresenter.apply(value);

        myUnseen.setVisible(Boolean.FALSE.equals(presentation.getSeen()));

        myTitle.setText(LocalizeValue.of(presentation.getTitle()));
        myTitle.setToolTipText(LocalizeValue.of(presentation.getTitle()));

        UserPresentation author = presentation.getAuthor();
        String prettyDate = DateFormatUtil.formatPrettyDate(presentation.getCreatedDate());
        if (author != null) {
            myInfo.setText(ReviewLocalize.reviewListInfoAuthor(presentation.getId(), prettyDate, author.getPresentableName()));
        }
        else {
            myInfo.setText(ReviewLocalize.reviewListInfo(presentation.getId(), prettyDate));
        }
        myInfo.setForegroundColor(item.isSelected() ? null : ComponentColors.DISABLED_TEXT);

        NamedCollection<TagPresentation> tagGroup = presentation.getTagGroup();
        myTags.setVisible(tagGroup != null);
        myTags.setToolTipText(tagGroup == null ? LocalizeValue.empty() : titledList(tagGroup, TagPresentation::getName));

        String state = presentation.getState();
        myStateLabel.setText(state == null ? LocalizeValue.empty() : LocalizeValue.of(state));
        myStateLabel.setVisible(state != null);

        showStatus(myNonMergeable, presentation.getMergeableStatus());
        showStatus(myBuildStatus, presentation.getBuildStatus());

        showUsersIcon(myUserGroup1, presentation.getUserGroup1());
        showUsersIcon(myUserGroup2, presentation.getUserGroup2());

        CommentsCounter counter = presentation.getCommentsCounter();
        myComments.setVisible(counter != null);
        myComments.setText(counter == null ? LocalizeValue.empty() : LocalizeValue.of(String.valueOf(counter.count())));
        myComments.setToolTipText(counter == null ? LocalizeValue.empty() : counter.tooltip());
    }

    @RequiredUIAccess
    private static void showStatus(Label label, @Nullable ReviewListItemStatus status) {
        label.setVisible(status != null);
        label.setImage(status == null ? null : status.icon());
        label.setToolTipText(status == null ? LocalizeValue.empty() : status.tooltip());
    }

    @RequiredUIAccess
    private static void showUsersIcon(HorizontalLayout layout, @Nullable NamedCollection<UserPresentation> users) {
        layout.removeAll();
        if (users == null) {
            layout.setVisible(false);
            return;
        }

        List<UserPresentation> items = users.getItems();
        for (UserPresentation user : items.subList(0, Math.min(MAX_PARTICIPANT_ICONS, items.size()))) {
            Label avatar = Label.create();
            avatar.setImage(ImageEffects.resize(user.getAvatarIcon(), AVATAR_SIZE));
            layout.add(avatar);
        }
        layout.setVisible(true);
        layout.setToolTipText(titledList(users, user -> LocalizeValue.of(user.getPresentableName())));
    }

    private static <E> LocalizeValue titledList(NamedCollection<E> collection, Function<E, LocalizeValue> nameFunction) {
        StringBuilder builder = new StringBuilder(collection.getNamePlural().get());
        for (E element : collection.getItems()) {
            builder.append('\n').append(nameFunction.apply(element).get());
        }
        return LocalizeValue.of(builder.toString());
    }
}
