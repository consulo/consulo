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

import consulo.review.details.data.ReviewState;
import consulo.review.submit.CodeReviewSubmitViewModel;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * The review the current user submits on a sand review; a body containing 'fail' is rejected with an error.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewSubmitViewModel implements CodeReviewSubmitViewModel {
    private final SandReviewFlowViewModel myFlowVm;
    private final ObservableValue<Integer> myDraftCommentsCount;
    private final MutableObservableValue<String> myText = MutableObservableValue.of("");
    private final MutableObservableValue<@Nullable Throwable> myError = MutableObservableValue.of(null);
    private volatile @Nullable CompletableFuture<?> myPopupFuture;

    public SandReviewSubmitViewModel(SandReviewFlowViewModel flowVm, int draftCommentsCount) {
        myFlowVm = flowVm;
        myDraftCommentsCount = ObservableValue.of(draftCommentsCount);
    }

    public void setPopupFuture(CompletableFuture<?> popupFuture) {
        myPopupFuture = popupFuture;
    }

    @Override
    public ObservableValue<Integer> getDraftCommentsCount() {
        return myDraftCommentsCount;
    }

    @Override
    public MutableObservableValue<String> getText() {
        return myText;
    }

    @Override
    public ObservableValue<Boolean> isBusy() {
        return myFlowVm.isBusy();
    }

    @Override
    public ObservableValue<@Nullable Throwable> getError() {
        return myError;
    }

    public void submit(ReviewState state) {
        if (myText.get().contains("fail")) {
            myError.set(new IllegalStateException("The sand review service rejects reviews containing 'fail'"));
            return;
        }
        myError.set(null);
        myFlowVm.submitReview(state, this::cancel);
    }

    @Override
    public void cancel() {
        CompletableFuture<?> popupFuture = myPopupFuture;
        if (popupFuture != null) {
            popupFuture.cancel(false);
        }
    }
}
