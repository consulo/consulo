// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiscussionsViewModel;
import kotlinx.coroutines.flow.StateFlow;

public interface CodeReviewInEditorViewModel extends CodeReviewDiscussionsViewModel {
    /**
     * true means that the local file state is out of sync with the server
     */
    StateFlow<Boolean> getUpdateRequired();

    void updateBranch();
}
