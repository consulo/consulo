// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.diff.action;

import consulo.review.diff.DiscussionsViewOption;
import consulo.review.diff.model.CodeReviewDiscussionsViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

final class CodeReviewDiscussionsToggleOptionAction extends ToggleAction {
    private final CodeReviewDiscussionsViewModel myVm;
    private final DiscussionsViewOption myViewOption;

    CodeReviewDiscussionsToggleOptionAction(CodeReviewDiscussionsViewModel vm, DiscussionsViewOption viewOption) {
        super(viewOption.toActionName());
        myVm = vm;
        myViewOption = viewOption;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myVm.getDiscussionsViewOption().get() == myViewOption;
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        myVm.setDiscussionsViewOption(myViewOption);
    }
}
