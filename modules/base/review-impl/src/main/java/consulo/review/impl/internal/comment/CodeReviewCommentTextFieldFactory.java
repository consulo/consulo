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

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.language.Language;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.file.LanguageFileType;
import consulo.language.plain.PlainTextLanguage;
import consulo.localize.LocalizeValue;
import consulo.review.comment.CodeReviewCommentSubmitAction;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.comment.CodeReviewSubmittableTextViewModel;
import consulo.review.comment.CodeReviewTextEditingViewModel;
import consulo.review.list.UserPresentation;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.ComputedResult;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The input of a new review comment - a timeline comment, a thread reply or a line comment. Sending is locked while
 * the view model submits, and a failed submit shows its reason under the input.
 * <p>
 * A view model with an {@linkplain CodeReviewSubmittableTextViewModel#getEditorSetup() editor setup} is written in a
 * markdown editor sent with {@code Ctrl+Enter}, otherwise in a message input sent with {@code Enter}.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewCommentTextFieldFactory {
    /**
     * What the submit hint under an editor input tells.
     */
    public enum HintKind {
        COMMENT,
        REPLY
    }

    private CodeReviewCommentTextFieldFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable,
                                   CodeReviewNewCommentViewModel vm,
                                   LocalizeValue placeholder,
                                   LocalizeValue sendText,
                                   @Nullable Component suffix) {
        return create(parentDisposable, vm, placeholder, sendText, suffix, HintKind.COMMENT);
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable,
                                   CodeReviewNewCommentViewModel vm,
                                   LocalizeValue placeholder,
                                   LocalizeValue sendText,
                                   @Nullable Component suffix,
                                   HintKind hintKind) {
        Consumer<Editor> editorSetup = vm.getEditorSetup();
        Component input = editorSetup == null
            ? createMessageInput(parentDisposable, vm, placeholder, sendText, suffix)
            : createEditorInput(parentDisposable, vm, editorSetup, placeholder, sendText, suffix, hintKind);
        return withAvatar(vm, input);
    }

    @RequiredUIAccess
    public static Component createEdit(Disposable parentDisposable, CodeReviewTextEditingViewModel vm) {
        Consumer<Editor> editorSetup = vm.getEditorSetup();
        return editorSetup == null ? createEditMessageInput(parentDisposable, vm) : createEditEditorInput(parentDisposable, vm, editorSetup);
    }

    @RequiredUIAccess
    private static Component createMessageInput(Disposable parentDisposable,
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
            submit(vm, submitActions.get());
        });
        ReviewUIUtil.bind(parentDisposable, vm.getCommentSubmitActions(), actions -> {
            submitActions.set(actions);
            input.setSendText(actions.isEmpty() ? sendText : actions.get(0).text());
            fillSecondaryActions(secondarySubmitActions, actions);
        });
        vm.addFocusRequestListener(parentDisposable, input::focus);

        Label error = createErrorLabel();

        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            input.setEnabled(state == null || !state.isInProgress());
            showError(error, state);
            restoreFailedSubmit(input, state, errorOf(state), inputText, submittedText);
        });

        DockLayout panel = DockLayout.create(Space.X_SMALL);
        panel.center(input);
        panel.bottom(error);
        return panel;
    }

    @RequiredUIAccess
    private static Component createEditorInput(Disposable parentDisposable,
                                               CodeReviewNewCommentViewModel vm,
                                               Consumer<Editor> editorSetup,
                                               LocalizeValue placeholder,
                                               LocalizeValue sendText,
                                               @Nullable Component suffix,
                                               HintKind hintKind) {
        EditorBox editor = createEditor(parentDisposable, vm, editorSetup, placeholder);

        AtomicReference<List<CodeReviewCommentSubmitAction>> submitActions = new AtomicReference<>(List.of());
        Button submit = Button.create(sendText, event -> submit(vm, submitActions.get()));
        submit.addStyle(ButtonStyle.PRIMARY);

        HorizontalLayout secondarySubmitActions = HorizontalLayout.create(Space.MEDIUM);
        secondarySubmitActions.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        secondarySubmitActions.setVisible(false);
        ReviewUIUtil.bind(parentDisposable, vm.getCommentSubmitActions(), actions -> {
            submitActions.set(actions);
            submit.setText(actions.isEmpty() ? sendText : actions.get(0).text());
            fillSecondaryActions(secondarySubmitActions, actions);
        });

        HorizontalLayout actions = HorizontalLayout.create(Space.MEDIUM);
        actions.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        actions.add(secondarySubmitActions);
        if (vm.canSuggestChange()) {
            actions.add(Hyperlink.create(ReviewLocalize.reviewSuggestedChangeInsertAction(),
                event -> insertText(editor, vm.createSuggestedChange())));
        }
        if (suffix != null) {
            actions.add(suffix);
        }
        actions.add(submit);

        AnAction submitAction = DumbAwareAction.create(event -> {
            if (submit.isEnabled()) {
                submit(vm, submitActions.get());
            }
        });
        submitAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor, parentDisposable);

        Label error = createErrorLabel();
        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            boolean enabled = state == null || !state.isInProgress();
            editor.setEnabled(enabled);
            submit.setEnabled(enabled);
            showError(error, state);
        });

        return layoutEditorInput(editor, createHint(hintKind), actions, error);
    }

    @RequiredUIAccess
    private static Component createEditEditorInput(Disposable parentDisposable,
                                                   CodeReviewTextEditingViewModel vm,
                                                   Consumer<Editor> editorSetup) {
        EditorBox editor = createEditor(parentDisposable, vm, editorSetup, LocalizeValue.empty());

        Button save = Button.create(ReviewLocalize.reviewCommentSave(), event -> vm.save());
        save.addStyle(ButtonStyle.PRIMARY);

        HorizontalLayout actions = HorizontalLayout.create(Space.MEDIUM);
        actions.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        actions.add(Hyperlink.create(ReviewLocalize.reviewCommentCancel(), event -> vm.stopEditing()));
        actions.add(save);

        AnAction saveAction = DumbAwareAction.create(event -> {
            if (save.isEnabled()) {
                vm.save();
            }
        });
        saveAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor, parentDisposable);
        AnAction cancelAction = DumbAwareAction.create(event -> vm.stopEditing());
        cancelAction.registerCustomShortcutSet(CommonShortcuts.ESCAPE, editor, parentDisposable);

        Label error = createErrorLabel();
        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            boolean enabled = state == null || !state.isInProgress();
            editor.setEnabled(enabled);
            save.setEnabled(enabled);
            showError(error, state);
        });

        return layoutEditorInput(editor, Label.create(), actions, error);
    }

    /**
     * A markdown editor - plain text without the markdown support - holding the text of the view model.
     */
    @RequiredUIAccess
    private static EditorBox createEditor(Disposable parentDisposable,
                                          CodeReviewSubmittableTextViewModel vm,
                                          Consumer<Editor> editorSetup,
                                          LocalizeValue placeholder) {
        EditorBox editor = vm.getProject().getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(vm.getProject())
            .language(markdownLanguage())
            .text(vm.getText().get())
            .multiline()
            .placeholder(placeholder)
            .customize(editorSetup::accept)
            .build();

        editor.addValueListener(event -> {
            String text = event.getValue();
            if (!vm.getText().get().equals(text)) {
                vm.getText().set(text == null ? "" : text);
            }
        });
        ReviewUIUtil.bind(parentDisposable, vm.getText(), text -> {
            if (!text.equals(editor.getValue())) {
                editor.setValue(text);
            }
        });
        vm.addFocusRequestListener(parentDisposable, editor::focus);
        return editor;
    }

    private static Language markdownLanguage() {
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByExtension("md");
        return fileType instanceof LanguageFileType languageFileType ? languageFileType.getLanguage() : PlainTextLanguage.INSTANCE;
    }

    @RequiredUIAccess
    private static void insertText(EditorBox editor, String text) {
        String current = editor.getValue();
        editor.setValue(current == null || current.isEmpty() ? text : current + "\n" + text);
        editor.focus();
    }

    @RequiredUIAccess
    private static Component layoutEditorInput(EditorBox editor, Label hint, Component actions, Label error) {
        DockLayout actionsRow = DockLayout.create(Space.MEDIUM);
        actionsRow.left(hint);
        actionsRow.right(actions);

        VerticalLayout panel = VerticalLayout.create(Space.X_SMALL);
        panel.add(editor);
        panel.add(actionsRow);
        panel.add(error);
        return panel;
    }

    @RequiredUIAccess
    private static Label createHint(HintKind hintKind) {
        String shortcut = KeymapUtil.getShortcutsText(CommonShortcuts.CTRL_ENTER.getShortcuts());
        Label hint = Label.create(hintKind == HintKind.REPLY ? ReviewLocalize.reviewCommentsReplyHint(shortcut) : ReviewLocalize.reviewCommentHint(shortcut));
        hint.addStyle(LabelStyle.FONT_SMALL);
        hint.setForegroundColor(ComponentColors.DISABLED_TEXT);
        return hint;
    }

    /**
     * The picture of who writes, before the input.
     */
    @RequiredUIAccess
    private static Component withAvatar(CodeReviewSubmittableTextViewModel vm, Component input) {
        UserPresentation user = vm.getCurrentUserPresentation();
        if (user == null) {
            return input;
        }
        Avatar avatar = Avatar.create(LocalizeValue.of(user.getPresentableName()), user.getAvatarIcon());
        avatar.addStyle(AvatarStyle.SMALL);
        VerticalLayout avatarBox = VerticalLayout.create(Space.NONE).add(avatar);

        DockLayout row = DockLayout.create(Space.MEDIUM);
        row.left(avatarBox);
        row.center(input);
        return row;
    }

    private static void submit(CodeReviewNewCommentViewModel vm, List<CodeReviewCommentSubmitAction> actions) {
        if (actions.isEmpty()) {
            vm.submit();
        }
        else {
            actions.get(0).action().run();
        }
    }

    @RequiredUIAccess
    private static void fillSecondaryActions(HorizontalLayout secondarySubmitActions, List<CodeReviewCommentSubmitAction> actions) {
        secondarySubmitActions.removeAll();
        for (int i = 1; i < actions.size(); i++) {
            CodeReviewCommentSubmitAction action = actions.get(i);
            secondarySubmitActions.add(Hyperlink.create(action.text(), event -> action.action().run()));
        }
        secondarySubmitActions.setVisible(actions.size() > 1);
    }

    @RequiredUIAccess
    private static Label createErrorLabel() {
        Label error = Label.create();
        error.addStyle(LabelStyle.FONT_SMALL);
        error.setForegroundColor(StandardColors.RED);
        error.setVisible(false);
        return error;
    }

    @RequiredUIAccess
    private static void showError(Label error, @Nullable ComputedResult<?> state) {
        String message = errorOf(state);
        error.setText(message == null ? LocalizeValue.empty() : LocalizeValue.of(message));
        error.setVisible(message != null);
    }

    @RequiredUIAccess
    private static Component createEditMessageInput(Disposable parentDisposable, CodeReviewTextEditingViewModel vm) {
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

        Label error = createErrorLabel();

        ReviewUIUtil.bind(parentDisposable, vm.getState(), state -> {
            input.setEnabled(state == null || !state.isInProgress());
            showError(error, state);
            restoreFailedSubmit(input, state, errorOf(state), inputText, submittedText);
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
