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
import consulo.review.comment.CodeReviewCommentSubmitAction;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        HorizontalLayout secondarySubmitActions = HorizontalLayout.create(Space.MEDIUM);
        secondarySubmitActions.setVisible(false);
        if (vm.canSuggestChange()) {
            Hyperlink insertSuggestion = Hyperlink.create(ReviewLocalize.reviewSuggestedChangeInsertAction(),
                event -> input.insertText(vm.createSuggestedChange()));
            HorizontalLayout suffixes = HorizontalLayout.create(Space.MEDIUM).add(secondarySubmitActions).add(insertSuggestion);
            if (suffix != null) {
                suffixes.add(suffix);
            }
            input.setSuffixComponent(suffixes);
        }
        else if (suffix != null) {
            input.setSuffixComponent(HorizontalLayout.create(Space.MEDIUM).add(secondarySubmitActions).add(suffix));
        }
        AtomicReference<List<CodeReviewCommentSubmitAction>> submitActions = new AtomicReference<>(List.of());
        AtomicReference<String> inputText = new AtomicReference<>("");
        AtomicReference<@Nullable String> submittedText = new AtomicReference<>();
        input.addTextChangeListener(event -> {
            inputText.set(event.getValue());
            vm.getText().set(event.getValue());
        });
        input.addSubmitListener(event -> {
            submittedText.set(event.getValue());
            vm.getText().set(event.getValue());
            List<CodeReviewCommentSubmitAction> actions = submitActions.get();
            if (actions.isEmpty()) {
                vm.submit();
            }
            else {
                actions.get(0).action().run();
            }
        });
        ReviewUIUtil.bind(parentDisposable, vm.getCommentSubmitActions(), actions -> {
            submitActions.set(actions);
            input.setSendText(actions.isEmpty() ? sendText : actions.get(0).text());
            secondarySubmitActions.removeAll();
            for (int i = 1; i < actions.size(); i++) {
                CodeReviewCommentSubmitAction action = actions.get(i);
                secondarySubmitActions.add(Hyperlink.create(action.text(), event -> action.action().run()));
            }
            secondarySubmitActions.setVisible(actions.size() > 1);
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
            restoreFailedSubmit(input, state, message, inputText, submittedText);
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
        AtomicReference<String> inputText = new AtomicReference<>(vm.getText().get());
        AtomicReference<@Nullable String> submittedText = new AtomicReference<>();
        input.addTextChangeListener(event -> {
            inputText.set(event.getValue());
            vm.getText().set(event.getValue());
        });
        input.addSubmitListener(event -> {
            submittedText.set(event.getValue());
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
            restoreFailedSubmit(input, state, message, inputText, submittedText);
        });

        DockLayout panel = DockLayout.create(Space.X_SMALL);
        panel.center(input);
        panel.bottom(error);
        return panel;
    }

    /**
     * The input clears itself on send, so a failed submit puts the text back - unless something was typed meanwhile.
     */
    @RequiredUIAccess
    private static void restoreFailedSubmit(MessageInput input,
                                            @Nullable ComputedResult<?> state,
                                            @Nullable String errorMessage,
                                            AtomicReference<String> inputText,
                                            AtomicReference<@Nullable String> submittedText) {
        if (state == null || state.isInProgress()) {
            return;
        }
        String submitted = submittedText.getAndSet(null);
        if (errorMessage != null && submitted != null && !submitted.isEmpty() && inputText.get().isEmpty()) {
            input.insertText(submitted);
        }
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
