// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

@ApiStatus.Internal
public interface CodeReviewNavigableEditorViewModel {
    boolean getCanNavigate();

    @RequiresEdt
    boolean canGotoNextComment(@Nonnull String threadId);

    @RequiresEdt
    boolean canGotoNextComment(int line);

    @RequiresEdt
    boolean canGotoPreviousComment(@Nonnull String threadId);

    @RequiresEdt
    boolean canGotoPreviousComment(int line);

    @RequiresEdt
    void gotoNextComment(@Nonnull String threadId);

    @RequiresEdt
    void gotoNextComment(int line);

    @RequiresEdt
    void gotoPreviousComment(@Nonnull String threadId);

    @RequiresEdt
    void gotoPreviousComment(int line);

    Key<CodeReviewNavigableEditorViewModel> KEY = Key.create("CodeReview.Navigable.Editor.ViewModel");
}
