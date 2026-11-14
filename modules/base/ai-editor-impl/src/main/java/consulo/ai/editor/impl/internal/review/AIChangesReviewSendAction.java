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
package consulo.ai.editor.impl.internal.review;

import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.localize.LocalizeValue;
import consulo.review.icon.ReviewIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * Sends the comments on the changes to the agent - in the toolbar of their diff.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class AIChangesReviewSendAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final AIChangesReview myReview;

    public AIChangesReviewSendAction(AIChangesReview review) {
        super(AIEditorLocalize.buttonChangesReviewSend(), AIEditorLocalize.buttonChangesReviewSendDescription(), ReviewIconGroup.comment());
        myReview = review;
    }

    @Override
    public void update(AnActionEvent e) {
        int count = myReview.getDraftCount();
        e.getPresentation().setEnabled(count > 0);
        e.getPresentation().setText(count == 0
            ? AIEditorLocalize.buttonChangesReviewSend()
            : LocalizeValue.join(AIEditorLocalize.buttonChangesReviewSend(), LocalizeValue.of(" (" + count + ")")));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        myReview.send();
    }
}
