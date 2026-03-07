// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.error;

import com.intellij.collaboration.ui.HtmlEditorPaneUtil;
import consulo.application.util.HtmlChunk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ErrorStatusPanelFactory {
    private ErrorStatusPanelFactory() {
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull T error,
        @Nonnull ErrorStatusPresenter<T> errorPresenter
    ) {
        return create(error, errorPresenter, Alignment.CENTER);
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull T error,
        @Nonnull ErrorStatusPresenter<T> errorPresenter,
        @Nonnull Alignment alignment
    ) {
        Action[] actionHolder = new Action[]{null};
        JEditorPane pane = createPane(() -> actionHolder[0]);
        update(pane, alignment, error, errorPresenter, action -> actionHolder[0] = action);
        return pane;
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<T> errorState,
        @Nonnull ErrorStatusPresenter<T> errorPresenter
    ) {
        return create(scope, errorState, errorPresenter, Alignment.CENTER);
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<T> errorState,
        @Nonnull ErrorStatusPresenter<T> errorPresenter,
        @Nonnull Alignment alignment
    ) {
        Action[] actionHolder = new Action[]{null};
        JEditorPane pane = createPane(() -> actionHolder[0]);
        kotlinx.coroutines.BuildersKt.launch(
            scope,
            null,
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> {
                kotlinx.coroutines.flow.FlowKt.collect(errorState, error -> {
                    update(pane, alignment, error, errorPresenter, action -> actionHolder[0] = action);
                    return kotlin.Unit.INSTANCE;
                }, continuation);
                return kotlin.Unit.INSTANCE;
            }
        );
        return pane;
    }

    private static @Nonnull JEditorPane createPane(@Nonnull Supplier<@Nullable Action> getAction) {
        JEditorPane htmlEditorPane = new JEditorPane();
        htmlEditorPane.setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().build());
        htmlEditorPane.setForeground(NamedColorUtil.getErrorForeground());
        htmlEditorPane.setFocusable(true);
        htmlEditorPane.setEditable(false);
        htmlEditorPane.setOpaque(false);

        htmlEditorPane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(@Nonnull HyperlinkEvent event) {
                if (ErrorStatusPresenter.ERROR_ACTION_HREF.equals(event.getDescription())) {
                    ActionEvent actionEvent = new ActionEvent(htmlEditorPane, ActionEvent.ACTION_PERFORMED, "perform");
                    Action action = getAction.get();
                    if (action != null) {
                        action.actionPerformed(actionEvent);
                    }
                }
                else {
                    BrowserUtil.browse(event.getDescription());
                }
            }
        });
        htmlEditorPane.registerKeyboardAction(
            e -> {
                Action action = getAction.get();
                if (action != null) {
                    action.actionPerformed(e);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        );

        return htmlEditorPane;
    }

    @SuppressWarnings("unchecked")
    private static <T> void update(
        @Nonnull JEditorPane pane,
        @Nonnull Alignment alignment,
        @Nullable T error,
        @Nonnull ErrorStatusPresenter<T> errorPresenter,
        @Nonnull Consumer<@Nullable Action> setAction
    ) {
        if (error == null) {
            HtmlEditorPaneUtil.setHtmlBody(pane, "");
            pane.setVisible(false);
            return;
        }

        Action errorAction = errorPresenter.getErrorAction(error);
        setAction.accept(errorAction);

        String body;
        if (errorPresenter instanceof ErrorStatusPresenter.HTML) {
            body = ((ErrorStatusPresenter.HTML<T>) errorPresenter).getHTMLBody(error);
        }
        else {
            body = getErrorText(alignment, errorAction, error, (ErrorStatusPresenter.Text<T>) errorPresenter);
        }
        HtmlEditorPaneUtil.setHtmlBody(pane, body);
        pane.setVisible(true);
    }

    private static <T> @Nonnull String getErrorText(
        @Nonnull Alignment alignment,
        @Nullable Action errorAction,
        @Nonnull T error,
        @Nonnull ErrorStatusPresenter.Text<T> errorPresenter
    ) {
        HtmlBuilder errorTextBuilder = new HtmlBuilder();
        appendP(errorTextBuilder, alignment, errorPresenter.getErrorTitle(error));
        String errorDescription = errorPresenter.getErrorDescription(error);
        if (errorDescription != null) {
            appendP(errorTextBuilder, alignment, errorDescription);
        }

        if (errorAction != null) {
            String actionName = ActionUtilKt.getName(errorAction);
            appendP(
                errorTextBuilder,
                alignment,
                HtmlChunk.link(ErrorStatusPresenter.ERROR_ACTION_HREF, actionName != null ? actionName : "")
            );
        }

        return errorTextBuilder.wrapWithHtmlBody().toString();
    }

    private static void appendP(@Nonnull HtmlBuilder builder, @Nonnull Alignment alignment, @Nonnull HtmlChunk chunk) {
        builder.append(HtmlChunk.p().attr("align", alignment.getHtmlValue()).child(chunk));
    }

    private static void appendP(@Nonnull HtmlBuilder builder, @Nonnull Alignment alignment, @Nonnull @Nls String text) {
        appendP(builder, alignment, HtmlChunk.text(text));
    }

    public enum Alignment {
        LEFT("left"),
        CENTER("center");

        private final String htmlValue;

        Alignment(@Nonnull String htmlValue) {
            this.htmlValue = htmlValue;
        }

        public @Nonnull String getHtmlValue() {
            return htmlValue;
        }
    }
}
