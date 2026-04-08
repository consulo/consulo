// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline.thread;

import com.intellij.collaboration.ui.codereview.user.CodeReviewUser;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public interface CodeReviewFoldableThreadViewModel {
    @Nonnull
    StateFlow<RepliesStateData> getRepliesState();

    @Nonnull
    StateFlow<Boolean> getCanCreateReplies();

    @Nonnull
    StateFlow<Boolean> getIsBusy();

    @Nonnull
    StateFlow<Boolean> getRepliesFolded();

    void unfoldReplies();

    interface RepliesStateData {
        @Nonnull
        Set<CodeReviewUser> getRepliesAuthors();

        int getRepliesCount();

        @Nullable
        Date getLastReplyDate();
    }

    @ApiStatus.Internal
        record Default(@Nonnull Set<CodeReviewUser> repliesAuthors,
                       int repliesCount,
                       @Nullable Date lastReplyDate) implements RepliesStateData {
        @Override
        public @Nonnull Set<CodeReviewUser> getRepliesAuthors() {
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

    final class Empty implements RepliesStateData {
        public static final Empty INSTANCE = new Empty();

        private Empty() {
        }

        @Override
        public @Nonnull Set<CodeReviewUser> getRepliesAuthors() {
            return Collections.emptySet();
        }

        @Override
        public int getRepliesCount() {
            return 0;
        }

        @Override
        public @Nullable Date getLastReplyDate() {
            return null;
        }
    }
}
