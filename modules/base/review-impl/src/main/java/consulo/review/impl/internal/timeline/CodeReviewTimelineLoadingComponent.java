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
package consulo.review.impl.internal.timeline;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.list.error.ErrorStatusAlignment;
import consulo.review.list.error.ErrorStatusPanelFactory;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.util.ComputedResult;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.function.Supplier;

/**
 * Shows the timeline once the review is loaded - the loading or the failure until then.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public final class CodeReviewTimelineLoadingComponent {
    private CodeReviewTimelineLoadingComponent() {
    }

    /**
     * @param onLoaded called once the timeline is shown
     */
    @RequiredUIAccess
    public static Component create(Disposable parentDisposable,
                                   ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> tabVmState,
                                   Supplier<CodeReviewTimelineViewModel> timelineVm,
                                   Runnable onLoaded) {
        DockLayout panel = DockLayout.create();
        UIAccess uiAccess = UIAccess.current();
        Disposable stateDisposable = Disposable.newDisposable("CodeReviewTimelineLoadingComponent state");
        Disposer.register(parentDisposable, stateDisposable);
        boolean[] shown = {false};
        Runnable listener = tabVmState.addListener(result -> uiAccess.give(() -> {
            if (!shown[0] && !Disposer.isDisposed(stateDisposable)) {
                shown[0] = show(parentDisposable, panel, result, timelineVm, onLoaded);
                if (shown[0]) {
                    Disposer.dispose(stateDisposable);
                }
            }
        }));
        Disposer.register(stateDisposable, listener::run);
        if (show(parentDisposable, panel, tabVmState.get(), timelineVm, onLoaded)) {
            shown[0] = true;
            Disposer.dispose(stateDisposable);
        }
        return panel;
    }

    /**
     * @return whether the timeline is shown, so the state is not needed any more
     */
    @RequiredUIAccess
    private static boolean show(Disposable parentDisposable,
                                DockLayout panel,
                                ComputedResult<CodeReviewDetailsTabViewModel> result,
                                Supplier<CodeReviewTimelineViewModel> timelineVm,
                                Runnable onLoaded) {
        CodeReviewDetailsTabViewModel tabVm = result.getOrNull();
        Throwable error = result.exceptionOrNull();
        if (tabVm != null) {
            panel.center(CodeReviewTimelineComponent.create(parentDisposable, tabVm, timelineVm.get()));
            onLoaded.run();
            return true;
        }

        if (error != null) {
            ErrorStatusPanelFactory factory = Application.get().getInstance(ErrorStatusPanelFactory.class);
            Component errorPanel = factory.create(error,
                ErrorStatusPresenter.<Throwable>simple(ReviewLocalize.reviewTimelineDetailsCannotLoad()),
                ErrorStatusAlignment.CENTER);
            panel.center(centered(errorPanel));
        }
        else {
            Label loading = Label.create(ReviewLocalize.reviewTimelineLoading());
            loading.setImage(Image.busy());
            loading.setForegroundColor(ComponentColors.DISABLED_TEXT);
            panel.center(centered(loading));
        }
        return false;
    }

    @RequiredUIAccess
    private static Component centered(Component component) {
        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        layout.add(component);
        layout.paddingBuilder().allSet(Space.X_LARGE).apply();
        return layout;
    }
}
