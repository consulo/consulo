// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

public interface CodeReviewNavigableEditorViewModel {
    Key<CodeReviewNavigableEditorViewModel> KEY = Key.create("CodeReview.Navigable.Editor.ViewModel");

    boolean canNavigate();

    @RequiredUIAccess
    boolean canGotoNextComment(String threadId);

    @RequiredUIAccess
    boolean canGotoNextComment(int line);

    @RequiredUIAccess
    boolean canGotoPreviousComment(String threadId);

    @RequiredUIAccess
    boolean canGotoPreviousComment(int line);

    @RequiredUIAccess
    void gotoNextComment(String threadId);

    @RequiredUIAccess
    void gotoNextComment(int line);

    @RequiredUIAccess
    void gotoPreviousComment(String threadId);

    @RequiredUIAccess
    void gotoPreviousComment(int line);
}
