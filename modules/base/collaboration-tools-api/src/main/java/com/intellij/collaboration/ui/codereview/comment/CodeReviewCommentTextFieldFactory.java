// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.async.AsyncUtilKt;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldFactory;
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldFactory.ScrollOnChangePolicy;
import com.intellij.collaboration.ui.util.SwingActionUtilKt;
import com.intellij.collaboration.ui.util.SwingBindingsKt;
import com.intellij.collaboration.util.ComputedResultKt;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.update.Activatable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CodeReviewCommentTextFieldFactory {
    private CodeReviewCommentTextFieldFactory() {
    }

    public static @Nonnull JComponent createIn(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewSubmittableTextViewModel vm,
        @Nonnull CommentInputActionsComponentFactory.Config actions,
        @Nullable CommentTextFieldFactory.IconConfig icon
    ) {
        return createIn(cs, vm, actions, icon, editor -> {
        });
    }

    public static @Nonnull JComponent createIn(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewSubmittableTextViewModel vm,
        @Nonnull CommentInputActionsComponentFactory.Config actions,
        @Nullable CommentTextFieldFactory.IconConfig icon,
        @Nonnull Consumer<Editor> setupEditor
    ) {
        Editor editor = CodeReviewMarkdownEditor.create(vm.getProject());
        editor.getComponent().setOpaque(false);
        JBColor fieldBackground = JBColor.lazy(
            () -> editor.getComponent().isEnabled() ? UIUtil.getTextFieldBackground() : UIUtil.getTextFieldDisabledBackground()
        );
        editor.getComponent().setBackground(fieldBackground);
        if (editor instanceof EditorEx editorEx) {
            editorEx.setBackgroundColor(fieldBackground);
        }

        AsyncUtilKt.launchNow(cs, continuation -> {
            try {
                SwingBindingsKt.bindTextIn(editor.getDocument(), cs, vm.getText());
                kotlinx.coroutines.AwaitCancellationKt.awaitCancellation();
            }
            finally {
                EditorFactory.getInstance().releaseEditor(editor);
            }
            return null;
        });

        // also forces component revalidation on newline
        installScrollIfChangedController(editor, ScrollOnChangePolicy.ScrollToField);

        int textLength = editor.getDocument().getTextLength();
        if (textLength > 0) {
            editor.getSelectionModel().setSelection(0, textLength);
        }

        UiNotifyConnector.installOn(
            editor.getComponent(),
            new Activatable() {
                private Job focusListenerJob;

                @Override
                public void showNotify() {
                    focusListenerJob = AsyncUtilKt.launchNow(
                        cs,
                        continuation -> {
                            vm.getFocusRequests().collect(
                                unit -> {
                                    CollaborationToolsUIUtil.focusPanel(editor.getComponent());
                                    return null;
                                },
                                continuation
                            );
                            return null;
                        }
                    );
                }

                @Override
                public void hideNotify() {
                    if (focusListenerJob != null) {
                        focusListenerJob.cancel(null);
                    }
                }
            }
        );

        SingleValueModel<Boolean> busyValue = mapToValueModel(
            cs,
            vm.getState(),
            state -> state != null && state.isInProgress()
        );
        SingleValueModel<String> errorValue = mapToValueModel(
            cs,
            vm.getState(),
            state -> {
                if (state == null) {
                    return null;
                }
                Throwable ex = ComputedResultKt.exceptionOrNull(state);
                return ex != null ? ex.getLocalizedMessage() : null;
            }
        );

        busyValue.addAndInvokeListener(busy -> {
            editor.getContentComponent().setEnabled(!busy);
        });

        CollaborationToolsUIUtil.installValidator(editor.getComponent(), errorValue);

        setupEditor.accept(editor);

        JComponent editorComponent = UiDataProvider.wrapComponent(
            editor.getComponent(),
            sink -> {
                // required for undo/redo
                sink.set(PlatformCoreDataKeys.FILE_EDITOR, TextEditorProvider.getInstance().getTextEditor(editor));
            }
        );

        JComponent inputField = CollaborationToolsUIUtil.wrapWithProgressOverlay(editorComponent, busyValue);
        if (icon != null) {
            inputField = CommentTextFieldFactory.wrapWithLeftIcon(
                icon,
                inputField,
                () -> {
                    Insets borderInsets = editor.getComponent().getBorder().getBorderInsets(editor.getComponent());
                    return editor.getLineHeight() + borderInsets.top + borderInsets.bottom;
                }
            );
        }

        return CommentInputActionsComponentFactory.attachActions(cs, inputField, actions);
    }

    private static void installScrollIfChangedController(@Nonnull Editor editor, @Nonnull ScrollOnChangePolicy policy) {
        if (policy == ScrollOnChangePolicy.DontScroll) {
            return;
        }

        Runnable scroll = () -> {
            JComponent parent = editor.getComponent().getParent() instanceof JComponent p ? p : null;
            if (parent == null) {
                return;
            }
            if (policy instanceof ScrollOnChangePolicy.ScrollToComponent scrollToComponent) {
                JComponent componentToScroll = scrollToComponent.getComponent();
                parent.scrollRectToVisible(new Rectangle(0, 0, componentToScroll.getWidth(), componentToScroll.getHeight()));
            }
            else if (policy == ScrollOnChangePolicy.ScrollToField) {
                parent.scrollRectToVisible(new Rectangle(0, 0, parent.getWidth(), parent.getHeight()));
            }
        };

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@Nonnull DocumentEvent event) {
                scroll.run();
            }
        });

        editor.getComponent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component parent = e != null ? e.getComponent().getParent() : null;
                if (parent != null && UIUtil.isFocusAncestor(parent)) {
                    scroll.run();
                }
            }
        });
    }

    private static <T, R> @Nonnull SingleValueModel<R> mapToValueModel(
        @Nonnull CoroutineScope cs,
        @Nonnull StateFlow<T> flow,
        @Nonnull Function<T, R> mapper
    ) {
        SingleValueModel<R> model = new SingleValueModel<>(mapper.apply(flow.getValue()));
        kotlinx.coroutines.CoroutineScopeKt.launch(
            cs,
            kotlinx.coroutines.Dispatchers.getDefault(),
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (scope, continuation) -> {
                flow.collect(
                    value -> {
                        model.setValue(mapper.apply(value));
                        return null;
                    },
                    continuation
                );
                return null;
            }
        );
        return model;
    }

    public static @Nonnull <VM extends CodeReviewSubmittableTextViewModel> Action submitActionIn(
        @Nonnull CoroutineScope cs,
        @Nonnull VM vm,
        @Nls @Nonnull String name,
        @Nonnull Consumer<VM> doSubmit
    ) {
        Action action = SwingActionUtilKt.swingAction(name, e -> doSubmit.accept(vm));
        action.setEnabled(false);
        SwingBindingsKt.bindEnabledIn(
            action,
            cs,
            FlowKt.combine(
                vm.getText(),
                vm.getState(),
                (text, state) -> text != null && !text.isBlank() && (state == null || !state.isInProgress())
            )
        );
        return action;
    }
}
