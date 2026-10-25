// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.diff.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.review.diff.DiscussionsViewOption;
import consulo.review.diff.model.CodeReviewDiscussionsViewModel;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import org.jspecify.annotations.Nullable;

public abstract class CodeReviewDiscussionsToggleAction extends ActionGroup implements DumbAware, AnActionWithSyncUpdate {
    @Override
    public void update(AnActionEvent e) {
        CodeReviewDiscussionsViewModel vm = findViewModel(e.getDataContext());
        e.getPresentation().setEnabledAndVisible(vm != null);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        CodeReviewDiscussionsViewModel vm = e == null ? null : findViewModel(e.getDataContext());
        if (vm == null) {
            return EMPTY_ARRAY;
        }
        DiscussionsViewOption[] options = DiscussionsViewOption.values();
        AnAction[] actions = new AnAction[options.length];
        for (int i = 0; i < options.length; i++) {
            actions[i] = new CodeReviewDiscussionsToggleOptionAction(vm, options[i]);
        }
        return actions;
    }

    protected abstract @Nullable CodeReviewDiscussionsViewModel findViewModel(DataContext ctx);
}
