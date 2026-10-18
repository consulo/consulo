// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list;

import org.jspecify.annotations.Nullable;

import java.util.Date;

public record SimpleReviewListItemPresentation(String title,
                                               String id,
                                               Date createdDate,
                                               @Nullable UserPresentation author,
                                               @Nullable NamedCollection<TagPresentation> tagGroup,
                                               @Nullable ReviewListItemStatus mergeableStatus,
                                               @Nullable ReviewListItemStatus buildStatus,
                                               @Nullable String state,
                                               @Nullable NamedCollection<UserPresentation> userGroup1,
                                               @Nullable NamedCollection<UserPresentation> userGroup2,
                                               @Nullable CommentsCounter commentsCounter,
                                               @Nullable Boolean seen) implements ReviewListItemPresentation {
    public SimpleReviewListItemPresentation(String title, String id, Date createdDate) {
        this(title, id, createdDate, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public @Nullable UserPresentation getAuthor() {
        return author;
    }

    @Override
    public @Nullable NamedCollection<TagPresentation> getTagGroup() {
        return tagGroup;
    }

    @Override
    public @Nullable ReviewListItemStatus getMergeableStatus() {
        return mergeableStatus;
    }

    @Override
    public @Nullable ReviewListItemStatus getBuildStatus() {
        return buildStatus;
    }

    @Override
    public @Nullable String getState() {
        return state;
    }

    @Override
    public @Nullable NamedCollection<UserPresentation> getUserGroup1() {
        return userGroup1;
    }

    @Override
    public @Nullable NamedCollection<UserPresentation> getUserGroup2() {
        return userGroup2;
    }

    @Override
    public @Nullable CommentsCounter getCommentsCounter() {
        return commentsCounter;
    }

    @Override
    public @Nullable Boolean getSeen() {
        return seen;
    }
}
