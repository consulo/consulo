// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.impl.internal.changes;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeListViewModel;

/**
 * Shows the selected changes in the diff preview of the review.
 */
@ActionImpl(id = "CodeReview.ShowDiff", shortcutFrom = @ActionRef(id = "ChangesView.Diff"))
public class CodeReviewShowDiffAction extends CodeReviewShowDiffActionBase {
    public CodeReviewShowDiffAction() {
        super(ReviewLocalize.actionCodereviewShowdiffText(), ReviewLocalize.actionCodereviewShowdiffDescription(), PlatformIconGroup.actionsDiff());
    }

    @RequiredUIAccess
    @Override
    protected void doShowDiff(CodeReviewChangeListViewModel vm) {
        vm.showDiffPreview();
    }
}
