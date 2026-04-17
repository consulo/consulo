// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.timeline.thread;

import consulo.collaboration.util.ReadOnlyObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

/**
 * View model for a foldable thread with replies.
 */
public interface CodeReviewFoldableThreadViewModel {
    ReadOnlyObservableValue<RepliesStateData> getRepliesState();

    ReadOnlyObservableValue<Boolean> getCanCreateReplies();

    ReadOnlyObservableValue<Boolean> getIsBusy();

    ReadOnlyObservableValue<Boolean> getRepliesFolded();

    void unfoldReplies();

    interface RepliesStateData {
        Set<String> getRepliesAuthors();

        int getRepliesCount();

        @Nullable Date getLastReplyDate();
    }
}
