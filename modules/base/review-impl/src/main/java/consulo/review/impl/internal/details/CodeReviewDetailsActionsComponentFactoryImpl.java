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
package consulo.review.impl.internal.details;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.details.CodeReviewDetailsActionsComponentFactory;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.details.data.ReviewState;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@ServiceImpl
public class CodeReviewDetailsActionsComponentFactoryImpl implements CodeReviewDetailsActionsComponentFactory {
    private static final String PLACE = "Code.Review.Details.Actions";
    private static final String MORE_PLACE = "Code.Review.Details.Actions.More";

    private final JBPopupFactory myPopupFactory;
    private final DataManager myDataManager;

    @Inject
    public CodeReviewDetailsActionsComponentFactoryImpl(JBPopupFactory popupFactory, DataManager dataManager) {
        myPopupFactory = popupFactory;
        myDataManager = dataManager;
    }

    @RequiredUIAccess
    @Override
    public <Reviewer> Component createRequestReviewButton(Disposable parentDisposable,
                                                          ObservableValue<ReviewState> reviewState,
                                                          ObservableValue<List<Reviewer>> requestedReviewers,
                                                          AnAction requestReviewAction) {
        Button button = ReviewUIUtil.createActionButton(parentDisposable, requestReviewAction, PLACE);
        ReviewUIUtil.bind(parentDisposable, reviewState, requestedReviewers, (state, reviewers) ->
            button.setVisible(state == ReviewState.NEED_REVIEW || (state == ReviewState.WAIT_FOR_UPDATES && !reviewers.isEmpty())));
        return button;
    }

    @RequiredUIAccess
    @Override
    public <Reviewer> Component createReRequestReviewButton(Disposable parentDisposable,
                                                            ObservableValue<ReviewState> reviewState,
                                                            ObservableValue<List<Reviewer>> requestedReviewers,
                                                            AnAction reRequestReviewAction) {
        Button button = ReviewUIUtil.createActionButton(parentDisposable, reRequestReviewAction, PLACE);
        ReviewUIUtil.bind(parentDisposable, reviewState, requestedReviewers, (state, reviewers) ->
            button.setVisible(state == ReviewState.WAIT_FOR_UPDATES && reviewers.isEmpty()));
        return button;
    }

    @RequiredUIAccess
    @Override
    public Component createActionsComponent(Disposable parentDisposable,
                                            ObservableValue<ReviewRequestState> reviewRequestState,
                                            Component openedStatePanel,
                                            Component mergedStatePanel,
                                            Component closedStatePanel,
                                            Component draftedStatePanel) {
        VerticalLayout wrapper = VerticalLayout.create(Space.NONE);
        wrapper.add(openedStatePanel);
        wrapper.add(mergedStatePanel);
        wrapper.add(closedStatePanel);
        wrapper.add(draftedStatePanel);
        ReviewUIUtil.bind(parentDisposable, reviewRequestState, requestState -> {
            openedStatePanel.setVisible(requestState == ReviewRequestState.OPENED);
            mergedStatePanel.setVisible(requestState == ReviewRequestState.MERGED);
            closedStatePanel.setVisible(requestState == ReviewRequestState.CLOSED);
            draftedStatePanel.setVisible(requestState == ReviewRequestState.DRAFT);
        });
        return wrapper;
    }

    @RequiredUIAccess
    @Override
    public Component createActionsForMergedReview() {
        return HorizontalLayout.create(Space.NONE);
    }

    @RequiredUIAccess
    @Override
    public Component createActionsForClosedReview(Disposable parentDisposable, AnAction reopenReviewAction) {
        Button reopenReviewButton = ReviewUIUtil.createActionButton(parentDisposable, reopenReviewAction, PLACE);
        return HorizontalLayout.create(BUTTONS_GAP).add(reopenReviewButton);
    }

    @RequiredUIAccess
    @Override
    public Component createActionsForDraftReview(Disposable parentDisposable, AnAction postReviewAction) {
        Button postReviewButton = ReviewUIUtil.createActionButton(parentDisposable, postReviewAction, PLACE);
        return HorizontalLayout.create(BUTTONS_GAP).add(postReviewButton);
    }

    @RequiredUIAccess
    @Override
    public Component createMoreButton(ActionGroup actionGroup) {
        Button button = Button.create(LocalizeValue.empty());
        button.setIcon(PlatformIconGroup.actionsMorevertical());
        button.addStyle(ButtonStyle.INPLACE);
        button.addClickListener(event -> {
            ListPopup popup = myPopupFactory.createActionGroupPopup(
                null,
                actionGroup,
                myDataManager.getDataContext(button),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true,
                MORE_PLACE
            );
            popup.showBy(button, null, PopupPosition.BOTTOM);
        });
        return button;
    }
}
