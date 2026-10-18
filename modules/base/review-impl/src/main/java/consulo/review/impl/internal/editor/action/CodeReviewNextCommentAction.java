// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.editor.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.ex.action.IdeActions;

@ActionImpl(id = "CodeReview.NextComment", shortcutFrom = @ActionRef(id = IdeActions.ACTION_NEXT_OCCURENCE))
public class CodeReviewNextCommentAction extends CodeReviewNextPreviousCommentAction {
    public CodeReviewNextCommentAction() {
        super(
            ReviewLocalize.actionCodeReviewNextCommentText(),
            ReviewLocalize.actionCodeReviewNextCommentDescription(),
            PlatformIconGroup.actionsNextoccurence(),
            (model, line) -> model.canGotoNextComment(line),
            (model, line) -> model.gotoNextComment(line)
        );
    }
}
