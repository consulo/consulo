// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.review.diff.model.CodeReviewDiscussionsViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;

public interface CodeReviewInEditorViewModel extends CodeReviewDiscussionsViewModel {
    /**
     * true means that the local file state is out of sync with the server
     */
    ObservableValue<Boolean> getUpdateRequired();

    void updateBranch();
}
