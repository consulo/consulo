// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.review;

import consulo.application.AllIcons;
import consulo.collaboration.CollaborationToolsBundle;
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory;
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter;
import com.intellij.collaboration.ui.util.SwingBindingsKt;
import com.intellij.collaboration.ui.util.popup.PopupUtilKt;
import com.intellij.vcsUtil.VcsUtilKt;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.ComponentContainer;
import consulo.ui.ex.awt.HorizontalLayout;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.MutableStateFlow;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

@ApiStatus.Internal
public abstract class CodeReviewSubmitPopupHandler<VM extends CodeReviewSubmitViewModel> {

    protected static final int ACTIONS_GAP = 6;
    protected static final int TITLE_ACTIONS_GAP = 5;

    public suspend Object show(
        @Nonnull VM vm,
        @Nonnull Component parentComponent,
        boolean above,
        @Nonnull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation
    ) {
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.EDT,
            (scope, cont) -> {
                ComponentContainer container = createPopupComponent(scope, vm, getErrorPresenter());
                JBPopup popup = createPopup(container);

                if (above) {
                    VcsUtilKt.showAbove(popup, parentComponent);
                }
                else {
                    popup.showUnderneathOf(parentComponent);
                }
                PopupUtilKt.awaitClose(popup, cont);
                return null;
            },
            continuation
        );
    }

    public suspend Object show(
        @Nonnull VM vm,
        @Nonnull Project project,
        @Nonnull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation
    ) {
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.EDT,
            (scope, cont) -> {
                ComponentContainer container = createPopupComponent(scope, vm, getErrorPresenter());
                JBPopup popup = createPopup(container);

                popup.showCenteredInCurrentWindow(project);
                PopupUtilKt.awaitClose(popup, cont);
                return null;
            },
            continuation
        );
    }

    protected @Nonnull JComponent createTitleActionsComponentIn(@Nonnull CoroutineScope cs, @Nonnull VM vm) {
        InlineIconButton button = new InlineIconButton(PlatformIconGroup.actionsClose(), AllIcons.Actions.CloseHovered);
        button.setBorder(JBUI.Borders.empty(5));
        button.setActionListener(ActionListener.class.cast(e -> vm.cancel()));
        return button;
    }

    protected abstract @Nonnull JComponent createActionsComponent(@Nonnull CoroutineScope cs, @Nonnull VM vm);

    protected abstract @Nonnull ErrorStatusPresenter<Throwable> getErrorPresenter();

    private @Nonnull ComponentContainer createPopupComponent(
        @Nonnull CoroutineScope cs,
        @Nonnull VM vm,
        @Nonnull ErrorStatusPresenter<Throwable> errorPresenter
    ) {
        return new ComponentContainer() {
            private final EditorTextField editor = createEditor(vm.getText());
            private final JComponent panel = createPanel();

            @Override
            public @Nonnull JComponent getComponent() {
                return panel;
            }

            @Override
            public @Nonnull JComponent getPreferredFocusableComponent() {
                return editor;
            }

            @Override
            public void dispose() {
            }

            private @Nonnull JComponent createPanel() {
                JLabel titleLabel = new JLabel(CollaborationToolsLocalize.reviewSubmitReviewTitle().get());
                titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD));

                JComponent titleActions = createTitleActionsComponentIn(cs, vm);
                JPanel titlePanel = new JPanel(new HorizontalLayout(TITLE_ACTIONS_GAP));
                titlePanel.setOpaque(false);
                titlePanel.add(titleLabel, HorizontalLayout.LEFT);
                SwingBindingsKt.bindChildIn(
                    titlePanel,
                    cs,
                    vm.getDraftCommentsCount(),
                    HorizontalLayout.LEFT,
                    1,
                    count -> count <= 0
                        ? null
                        : new JLabel(CollaborationToolsBundle.message("review.pending.comments.count", count))
                );
                titlePanel.add(titleActions, HorizontalLayout.RIGHT);

                JComponent errorPanel =
                    ErrorStatusPanelFactory.create(cs, vm.getError(), errorPresenter, ErrorStatusPanelFactory.Alignment.LEFT);
                JComponent buttonsPanel = createActionsComponent(cs, vm);

                JPanel mainPanel = new JPanel(new MigLayout(new LC().insets("12").fill().flowY().noGrid().hideMode(3)));
                mainPanel.setBackground(JBUI.CurrentTheme.Popup.BACKGROUND);
                mainPanel.setPreferredSize(new JBDimension(500, 200));

                mainPanel.add(titlePanel, new CC().growX());
                mainPanel.add(editor, new CC().growX().growY());
                mainPanel.add(errorPanel, new CC().growY().growPrioY(0));
                mainPanel.add(buttonsPanel, new CC());
                return mainPanel;
            }

            private @Nonnull EditorTextField createEditor(@Nonnull MutableStateFlow<String> text) {
                EditorTextField field = new EditorTextField(text.getValue(), null, FileTypes.PLAIN_TEXT);
                field.setOneLineMode(false);
                field.setPlaceholder(CollaborationToolsLocalize.reviewCommentPlaceholder().get());
                field.addSettingsProvider(editorEx -> {
                    editorEx.getSettings().setUseSoftWraps(true);
                    editorEx.setVerticalScrollbarVisible(true);
                    editorEx.getScrollPane().setViewportBorder(JBUI.Borders.emptyLeft(4));
                    editorEx.putUserData(IncrementalFindAction.SEARCH_DISABLED, true);
                });
                SwingBindingsKt.bindTextIn(field.getDocument(), cs, text);
                return field;
            }
        };
    }

    private @Nonnull JBPopup createPopup(@Nonnull ComponentContainer container) {
        return JBPopupFactory.getInstance()
            // popup requires a properly focusable component, will not look under a panel
            .createComponentPopupBuilder(container.getComponent(), container.getPreferredFocusableComponent())
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .createPopup();
    }
}
