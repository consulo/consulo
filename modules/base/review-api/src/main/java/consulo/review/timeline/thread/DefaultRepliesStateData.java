// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.timeline.thread;

import consulo.review.user.CodeReviewUser;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

public record DefaultRepliesStateData(Set<CodeReviewUser> repliesAuthors, int repliesCount, @Nullable Date lastReplyDate)
    implements RepliesStateData {
    @Override
    public Set<CodeReviewUser> getRepliesAuthors() {
        return repliesAuthors;
    }

    @Override
    public int getRepliesCount() {
        return repliesCount;
    }

    @Override
    public @Nullable Date getLastReplyDate() {
        return lastReplyDate;
    }
}
