// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list;

import org.jspecify.annotations.Nullable;

import java.util.Date;

public interface ReviewListItemPresentation {
    String getTitle();

    String getId();

    Date getCreatedDate();

    @Nullable
    UserPresentation getAuthor();

    @Nullable
    NamedCollection<TagPresentation> getTagGroup();

    @Nullable
    ReviewListItemStatus getMergeableStatus();

    @Nullable
    ReviewListItemStatus getBuildStatus();

    @Nullable
    String getState();

    @Nullable
    NamedCollection<UserPresentation> getUserGroup1();

    @Nullable
    NamedCollection<UserPresentation> getUserGroup2();

    @Nullable
    CommentsCounter getCommentsCounter();

    @Nullable
    Boolean getSeen();
}
