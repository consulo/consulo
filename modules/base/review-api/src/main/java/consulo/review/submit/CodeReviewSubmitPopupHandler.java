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
package consulo.review.submit;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.review.list.error.ErrorStatusAlignment;
import consulo.review.list.error.ErrorStatusPanelFactory;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.ReviewUIUtil;
import consulo.review.util.popup.CollaborationToolsPopupUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Shows the popup with the body of the review and the buttons submitting it.
 * <p>
 * The returned future completes when the popup closes, and cancelling the future closes the popup.
 */
public abstract class CodeReviewSubmitPopupHandler<VM extends CodeReviewSubmitViewModel> {
    // gap 12 minus button borders (3x2)
    protected static final Space ACTIONS_GAP = Space.MEDIUM;
    protected static final Space TITLE_ACTIONS_GAP = Space.SMALL;

    private static final int POPUP_WIDTH = 500;
    private static final int POPUP_HEIGHT = 200;

    @RequiredUIAccess
    public CompletableFuture<?> show(VM vm, Component parentComponent) {
        return show(vm, parentComponent, false);
    }

    @RequiredUIAccess
    public CompletableFuture<?> show(VM vm, Component parentComponent, boolean above) {
        JBPopup popup = createPopup(vm);
        CompletableFuture<Object> result = new CompletableFuture<>();
        CollaborationToolsPopupUtil.completeOnClose(popup, result, () -> null);
        popup.showBy(parentComponent, null, above ? PopupPosition.TOP : PopupPosition.BOTTOM);
        return result;
    }

    @RequiredUIAccess
    public CompletableFuture<?> show(VM vm, Project project) {
        JBPopup popup = createPopup(vm);
        CompletableFuture<Object> result = new CompletableFuture<>();
        CollaborationToolsPopupUtil.completeOnClose(popup, result, () -> null);
        popup.showCenteredInCurrentWindow(project);
        return result;
    }

    @RequiredUIAccess
    protected Component createTitleActionsComponent(Disposable parentDisposable, VM vm) {
        Button closeButton = Button.create(LocalizeValue.empty(), event -> vm.cancel());
        closeButton.setIcon(PlatformIconGroup.actionsClose());
        closeButton.addStyle(ButtonStyle.INPLACE);
        return closeButton;
    }

    @RequiredUIAccess
    protected abstract Component createActionsComponent(Disposable parentDisposable, VM vm);

    protected abstract ErrorStatusPresenter<Throwable> getErrorPresenter();

    @RequiredUIAccess
    private JBPopup createPopup(VM vm) {
        Disposable disposable = Disposable.newDisposable("CodeReviewSubmitPopup");

        TextArea editor = createEditor(disposable, vm);
        Component panel = createPanel(disposable, vm, editor);

        // popup requires a properly focusable component, will not look under a panel
        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, editor)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .createPopup();
        Disposer.register(popup, disposable);
        return popup;
    }

    @RequiredUIAccess
    private Component createPanel(Disposable disposable, VM vm, TextArea editor) {
        Label titleLabel = Label.create(ReviewLocalize.reviewSubmitReviewTitle());
        titleLabel.addStyle(LabelStyle.FONT_BOLD);

        Label pendingCommentsLabel = Label.create();
        ReviewUIUtil.bind(disposable, vm.getDraftCommentsCount(), count -> {
            pendingCommentsLabel.setText(count <= 0 ? LocalizeValue.empty() : ReviewLocalize.reviewPendingCommentsCount(count));
            pendingCommentsLabel.setVisible(count > 0);
        });

        DockLayout titlePanel = DockLayout.create(TITLE_ACTIONS_GAP);
        titlePanel.left(HorizontalLayout.create(TITLE_ACTIONS_GAP).add(titleLabel).add(pendingCommentsLabel));
        titlePanel.right(createTitleActionsComponent(disposable, vm));

        Component errorPanel = Application.get().getInstance(ErrorStatusPanelFactory.class)
            .create(disposable, vm.getError(), getErrorPresenter(), ErrorStatusAlignment.LEFT);
        Component buttonsPanel = createActionsComponent(disposable, vm);

        VerticalLayout bottomPanel = VerticalLayout.create(ACTIONS_GAP);
        bottomPanel.add(errorPanel);
        bottomPanel.add(buttonsPanel);

        DockLayout panel = DockLayout.create(ACTIONS_GAP);
        panel.top(titlePanel);
        panel.center(editor);
        panel.bottom(bottomPanel);
        panel.paddingBuilder().allSet(Space.LARGE).apply();
        panel.setWidth(POPUP_WIDTH);
        panel.setHeight(POPUP_HEIGHT);
        return panel;
    }

    @RequiredUIAccess
    private static TextArea createEditor(Disposable disposable, CodeReviewSubmitViewModel vm) {
        TextArea editor = TextArea.create(vm.getText().get());
        editor.setPlaceholder(ReviewLocalize.reviewCommentPlaceholder());
        editor.addValueListener(event -> {
            String value = Objects.requireNonNullElse(event.getValue(), "");
            if (!value.equals(vm.getText().get())) {
                vm.getText().set(value);
            }
        });
        ReviewUIUtil.bind(disposable, vm.getText(), text -> {
            if (!text.equals(Objects.requireNonNullElse(editor.getValue(), ""))) {
                editor.setValue(text);
            }
        });
        ReviewUIUtil.bind(disposable, vm.isBusy(), busy -> editor.setEditable(!busy));
        return editor;
    }
}
