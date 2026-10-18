// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.timeline.thread;

import consulo.review.user.CodeReviewUser;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

public interface RepliesStateData {
    RepliesStateData EMPTY = new DefaultRepliesStateData(Set.of(), 0, null);

    Set<CodeReviewUser> getRepliesAuthors();

    int getRepliesCount();

    @Nullable
    Date getLastReplyDate();
}
