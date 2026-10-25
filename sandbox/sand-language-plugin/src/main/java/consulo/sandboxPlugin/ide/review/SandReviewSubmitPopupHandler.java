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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.details.data.ReviewState;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.review.submit.CodeReviewSubmitPopupHandler;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;

/**
 * Approve, request changes or comment on a sand review.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewSubmitPopupHandler extends CodeReviewSubmitPopupHandler<SandReviewSubmitViewModel> {
    @RequiredUIAccess
    @Override
    protected Component createActionsComponent(Disposable parentDisposable, SandReviewSubmitViewModel vm) {
        Button approveButton = Button.create(LocalizeValue.localizeTODO("Approve"), event -> vm.submit(ReviewState.ACCEPTED));
        approveButton.addStyle(ButtonStyle.PRIMARY);
        Button requestChangesButton = Button.create(LocalizeValue.localizeTODO("Request Changes"),
            event -> vm.submit(ReviewState.WAIT_FOR_UPDATES));
        Button commentButton = Button.create(ReviewLocalize.reviewSubmitAction(), event -> vm.submit(ReviewState.NEED_REVIEW));

        ReviewUIUtil.bind(parentDisposable, vm.isBusy(), busy -> {
            approveButton.setEnabled(!busy);
            requestChangesButton.setEnabled(!busy);
            commentButton.setEnabled(!busy);
        });
        return HorizontalLayout.create(ACTIONS_GAP).add(approveButton).add(requestChangesButton).add(commentButton);
    }

    @Override
    protected ErrorStatusPresenter<Throwable> getErrorPresenter() {
        return ErrorStatusPresenter.simple(ReviewLocalize.reviewSubmitFailed());
    }
}
