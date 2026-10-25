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
package consulo.review.impl.internal.comment;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.comment.CodeReviewTextEditingViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.review.util.ComputedResult;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.style.StandardColors;
import org.jspecify.annotations.Nullable;

/**
 * The input of a new review comment - a timeline comment, a thread reply or a line comment. Sending is locked while
 * the view model submits, and a failed submit shows its reason under the input.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewCommentTextFieldFactory {
    private CodeReviewCommentTextFieldFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable,
                                   CodeReviewNewCommentViewModel vm,
                                   LocalizeValue placeholder,
                                   LocalizeValue sendText,
                                   @Nullable Component suffix) {
        MessageInput input = MessageInput.create();
        input.setPlaceholder(placeholder);
        input.setSendText(sendText);
        if (vm.canSuggestChange()) {
            Hyperlink insertSuggestion = Hyperlink.create(ReviewLocalize.reviewSuggestedChangeInsertAction(),
                event -> input.insertText(vm.createSuggestedChange()));
            HorizontalLayout suffixes = HorizontalLayout.create(Space.MEDIUM).add(insertSuggestion);
            if (suffix != null) {
                suffixes.add(suffix);
            }
            input.setSuffixComponent(suffixes);
        }
        else if (suffix != null) {
            input.setSuffixComponent(suffix);
        }
        input.addTextChangeListener(event -> vm.getText().set(event.getValue()));
        input.addSubmitListener(event -> {
            vm.getText().set(event.getValue());
            vm.submit();
        });
        vm.addFocusRequestListener(parentDisposable, input::focus);

        Label error = Label.create();
        error.addStyle(LabelStyle.FONT_SMALL);
        error.setForegroundColor(StandardColors.RED);
        error.setVisible(false);

        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            input.setEnabled(state == null || !state.isInProgress());
            String message = errorOf(state);
            error.setText(message == null ? LocalizeValue.empty() : LocalizeValue.of(message));
            error.setVisible(message != null);
        });

        DockLayout panel = DockLayout.create(Space.X_SMALL);
        panel.center(input);
        panel.bottom(error);
        return panel;
    }

    @RequiredUIAccess
    public static Component createEdit(Disposable parentDisposable, CodeReviewTextEditingViewModel vm) {
        MessageInput input = MessageInput.create();
        input.setSendText(ReviewLocalize.reviewCommentSave());
        input.insertText(vm.getText().get());
        input.setSuffixComponent(Hyperlink.create(ReviewLocalize.reviewCommentCancel(), event -> vm.stopEditing()));
        input.addTextChangeListener(event -> vm.getText().set(event.getValue()));
        input.addSubmitListener(event -> {
            vm.getText().set(event.getValue());
            vm.save();
        });
        vm.addFocusRequestListener(parentDisposable, input::focus);

        Label error = Label.create();
        error.addStyle(LabelStyle.FONT_SMALL);
        error.setForegroundColor(StandardColors.RED);
        error.setVisible(false);

        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            input.setEnabled(state == null || !state.isInProgress());
            String message = errorOf(state);
            error.setText(message == null ? LocalizeValue.empty() : LocalizeValue.of(message));
            error.setVisible(message != null);
        });

        DockLayout panel = DockLayout.create(Space.X_SMALL);
        panel.center(input);
        panel.bottom(error);
        return panel;
    }

    private static @Nullable String errorOf(@Nullable ComputedResult<?> state) {
        if (state == null) {
            return null;
        }
        Throwable error = state.exceptionOrNull();
        if (error == null) {
            return null;
        }
        String message = error.getLocalizedMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
