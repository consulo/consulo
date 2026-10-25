/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.sandboxPlugin.ide.review;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.dataContext.DataContext;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.diff.action.CodeReviewDiscussionsToggleAction;
import consulo.review.diff.model.CodeReviewDiscussionsViewModel;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ActionImpl(id = "Sand.Review.Diff.Discussions", parents = @ActionParentRef(@ActionRef(id = IdeActions.DIFF_VIEWER_TOOLBAR)))
public class SandReviewDiscussionsToggleAction extends CodeReviewDiscussionsToggleAction {
    public SandReviewDiscussionsToggleAction() {
        getTemplatePresentation().setText(LocalizeValue.localizeTODO("Review Discussions"));
        getTemplatePresentation().setIcon(PlatformIconGroup.actionsShow());
        setPopup(true);
    }

    @Override
    protected @Nullable CodeReviewDiscussionsViewModel findViewModel(DataContext ctx) {
        DiffContext context = ctx.getData(DiffDataKeys.DIFF_CONTEXT);
        return context == null ? null : context.getUserData(SandReviewDiffProcessorViewModel.KEY);
    }
}
