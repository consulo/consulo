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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeListViewModel;
import org.jspecify.annotations.Nullable;

/**
 * Shows the diff of the changes selected in a review changes tree.
 */
public abstract class CodeReviewShowDiffActionBase extends DumbAwareAction implements AnActionWithSyncUpdate {
    protected CodeReviewShowDiffActionBase(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
        CodeReviewChangeListViewModel vm = e.getData(CodeReviewChangeListViewModel.DATA_KEY);
        e.getPresentation().setVisible(vm != null);
        e.getPresentation().setEnabled(vm != null && vm.getChangesSelection().get() != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        CodeReviewChangeListViewModel vm = e.getData(CodeReviewChangeListViewModel.DATA_KEY);
        if (vm == null) {
            return;
        }
        doShowDiff(vm);
    }

    @RequiredUIAccess
    protected abstract void doShowDiff(CodeReviewChangeListViewModel vm);
}
