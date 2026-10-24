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
package consulo.review.impl.internal.editor.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.diff.DiscussionsViewOption;
import consulo.review.editor.CodeReviewInEditorToolbarModel;
import consulo.review.editor.CodeReviewInEditorViewModel;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The review mode of an editor in its inspection widget: the mode as the text, a warning icon while the branch is out of
 * sync, and the options to show all comments, the unresolved ones or none as the popup.
 */
public class CodeReviewInEditorToolbarActionGroup extends ComboBoxAction implements DumbAware {
    private final CodeReviewInEditorViewModel myVm;
    private final @Nullable Supplier<LocalizeValue> myCustomWarningSupplier;

    private final AnAction myUpdateAction;
    private final AnAction myDisableReviewAction;
    private final AnAction myHideResolvedAction;
    private final AnAction myShowAllAction;

    public CodeReviewInEditorToolbarActionGroup(CodeReviewInEditorToolbarModel model) {
        myVm = model.vm();
        myCustomWarningSupplier = model.customWarningSupplier();
        myUpdateAction = new UpdateAction();
        myDisableReviewAction = new ViewOptionToggleAction(DiscussionsViewOption.DONT_SHOW, ReviewLocalize.reviewEditorActionDisableText());
        myHideResolvedAction = new ViewOptionToggleAction(DiscussionsViewOption.UNRESOLVED_ONLY,
            ReviewLocalize.reviewEditorActionShowUnresolvedText());
        myShowAllAction = new ViewOptionToggleAction(DiscussionsViewOption.ALL, ReviewLocalize.reviewEditorActionShowAllText());
    }

    @Override
    protected ActionGroup createPopupActionGroup(DataContext context) {
        return ActionGroup.of(myUpdateAction, AnSeparator.create(), myDisableReviewAction, myHideResolvedAction, myShowAllAction);
    }

    @Override
    public void update(AnActionEvent e) {
        boolean shown = myVm.getDiscussionsViewOption().get() != DiscussionsViewOption.DONT_SHOW;
        boolean synced = !myVm.getUpdateRequired().get();
        Presentation presentation = e.getPresentation();
        LocalizeValue customWarning = myCustomWarningSupplier == null ? null : myCustomWarningSupplier.get();
        presentation.setDescription(customWarning != null ? customWarning : ReviewLocalize.reviewEditorModeDescription());

        if (shown) {
            presentation.setText(ReviewLocalize.reviewEditorModeTitle());
            presentation.setIcon(customWarning != null || !synced ? PlatformIconGroup.generalWarning() : null);
        }
        else {
            presentation.setText(LocalizeValue.empty());
            presentation.setIcon(PlatformIconGroup.actionsPreview());
        }
    }

    private class UpdateAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private UpdateAction() {
            super(ReviewLocalize.reviewEditorActionUpdateText(), LocalizeValue.empty(), PlatformIconGroup.actionsCheckout());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myVm.getUpdateRequired().get());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myVm.updateBranch();
        }
    }

    private class ViewOptionToggleAction extends ToggleAction implements DumbAware {
        private final DiscussionsViewOption myOption;

        private ViewOptionToggleAction(DiscussionsViewOption option, LocalizeValue text) {
            super(text);
            myOption = option;
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            if (myOption == DiscussionsViewOption.DONT_SHOW) {
                e.getPresentation().setKeepPopupOnPerform(KeepPopupOnPerform.NEVER);
            }
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myVm.getDiscussionsViewOption().get() == myOption;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myVm.setDiscussionsViewOption(myOption);
        }
    }
}
