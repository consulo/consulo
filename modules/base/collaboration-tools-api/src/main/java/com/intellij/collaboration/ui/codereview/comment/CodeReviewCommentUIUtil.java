// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.FocusAwareClippingRoundedPanel;
import com.intellij.collaboration.ui.HorizontalListPanel;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil;
import com.intellij.collaboration.ui.codereview.timeline.thread.CodeReviewFoldableThreadViewModel;
import com.intellij.collaboration.ui.codereview.timeline.thread.CodeReviewResolvableItemViewModel;
import com.intellij.collaboration.ui.codereview.user.CodeReviewUser;
import com.intellij.collaboration.ui.icon.IconsProvider;
import com.intellij.collaboration.ui.util.SwingBindingsKt;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.ui.*;
import consulo.application.AllIcons;
import consulo.application.util.DateFormatUtil;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorKeys;
import consulo.collaboration.CollaborationToolsBundle;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.colorScheme.EditorColorsManager;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.VirtualFile;
import icons.CollaborationToolsIcons;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.function.Consumer;

public final class CodeReviewCommentUIUtil {
    public static final int INLAY_PADDING = 10;
    private static final int EDITOR_INLAY_PANEL_ARC = 10;

    public static final Color COMMENT_BUBBLE_BORDER_COLOR = JBColor.namedColor(
        "Review.ChatItem.BubblePanel.Border",
        JBColor.namedColor("EditorTabs.underTabsBorderColor", JBColor.border())
    );

    private CodeReviewCommentUIUtil() {
    }

    public static @Nonnull Insets getInlayPadding(@Nonnull CodeReviewChatItemUIUtil.ComponentType componentType) {
        Insets paddingInsets = componentType.getPaddingInsets();
        int top = INLAY_PADDING - paddingInsets.top;
        int bottom = INLAY_PADDING - paddingInsets.bottom;
        return JBInsets.create(top, 0).withBottom(bottom);
    }

    public static @Nonnull JPanel createEditorInlayPanel(@Nonnull JComponent component) {
        return createEditorInlayPanel(component, null);
    }

    @ApiStatus.Internal
    public static @Nonnull JPanel createEditorInlayPanel(@Nonnull JComponent component, @Nullable Color tint) {
        JBColor borderColor = JBColor.lazy(() -> {
            Color c = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.TEARLINE_COLOR);
            return c != null ? c : JBColor.border();
        });
        FocusAwareClippingRoundedPanel roundedPanel =
            new FocusAwareClippingRoundedPanel(EDITOR_INLAY_PANEL_ARC, borderColor, new BorderLayout());
        roundedPanel.setBackground(JBColor.lazy(() -> {
            var scheme = EditorColorsManager.getInstance().getGlobalScheme();
            if (tint != null) {
                return ColorUtil.blendColorsInRgb(scheme.getDefaultBackground(), tint, 0.1);
            }
            return scheme.getDefaultBackground();
        }));

        roundedPanel.setFocusCycleRoot(true);
        roundedPanel.setFocusTraversalPolicyProvider(true);
        roundedPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        // Escape to the editor / diff
        roundedPanel.setFocusTraversalKeys(
            KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
            Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0))
        );

        roundedPanel.add(UiDataProvider.wrapComponent(component, sink -> suppressOuterEditorData(sink)));

        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                roundedPanel.dispatchEvent(new ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED));
            }
        });
        return roundedPanel;
    }

    /**
     * We suppress the outer editor context because this will cause registered editor actions not to be usable.
     * This means they don't steal key events from this component.
     * Specifically; the TAB key tends to escape the focused component if Editor is registered.
     */
    private static void suppressOuterEditorData(@Nonnull DataSink sink) {
        DataKey<?>[] keys = {
            Editor.KEY,
            EditorKeys.HOST_EDITOR,
            Caret.KEY,
            VirtualFile.KEY,
            VirtualFile.KEY_OF_ARRAY,
            CommonDataKeys.LANGUAGE,
            PsiFile.KEY,
            PsiElement.KEY,
            PlatformCoreDataKeys.FILE_EDITOR,
            PlatformCoreDataKeys.PSI_ELEMENT_ARRAY
        };
        for (DataKey<?> key : keys) {
            sink.setNull(key);
        }
    }

    public static @Nonnull JComponent createPostNowButton(@Nonnull Consumer<ActionEvent> actionListener) {
        ActionLink link = new ActionLink(CollaborationToolsLocalize.reviewCommentsPostNowAction().get());
        link.setAutoHideOnDisable(false);
        link.addActionListener(e -> actionListener.accept(e));
        return link;
    }

    public static @Nonnull JComponent createDeleteCommentIconButton(@Nonnull Consumer<ActionEvent> actionListener) {
        Icon icon = CollaborationToolsIcons.Delete;
        Icon hoverIcon = CollaborationToolsIcons.DeleteHovered;
        InlineIconButton button = new InlineIconButton(icon, hoverIcon);
        button.setTooltip(CommonBundle.message("button.delete"));
        button.setActionListener(e -> {
            if (MessageDialogBuilder.yesNo(
                CollaborationToolsLocalize.reviewCommentsDeleteConfirmationTitle().get(),
                CollaborationToolsLocalize.reviewCommentsDeleteConfirmation().get()
            ).ask(button)) {
                actionListener.accept(e);
            }
        });
        return button;
    }

    public static @Nonnull InlineIconButton createEditButton(@Nonnull Consumer<ActionEvent> actionListener) {
        Icon icon = AllIcons.General.Inline_edit;
        Icon hoverIcon = AllIcons.General.Inline_edit_hovered;
        InlineIconButton button = new InlineIconButton(icon, hoverIcon);
        button.setTooltip(CommonBundle.message("button.edit"));
        button.setActionListener(e -> actionListener.accept(e));
        return button;
    }

    public static @Nonnull InlineIconButton createAddReactionButton(@Nonnull Consumer<ActionEvent> actionListener) {
        Icon icon = CollaborationToolsIcons.AddEmoji;
        Icon hoverIcon = CollaborationToolsIcons.AddEmojiHovered;
        InlineIconButton button = new InlineIconButton(icon, hoverIcon);
        button.setTooltip(CollaborationToolsLocalize.reviewCommentsReactionAddTooltip().get());
        button.setActionListener(e -> actionListener.accept(e));
        return button;
    }

    public static @Nonnull JComponent createFoldedThreadControlsIn(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewFoldableThreadViewModel vm,
        @Nonnull IconsProvider<CodeReviewUser> avatarIconsProvider
    ) {
        JLabel authorsLabel = new JLabel();
        SwingBindingsKt.bindVisibilityIn(authorsLabel, cs, FlowKt.map(vm.getRepliesState(), state -> state.getRepliesCount() > 0));
        SwingBindingsKt.bindIconIn(authorsLabel, cs, FlowKt.map(vm.getRepliesState(), state -> {
            var authors = state.getRepliesAuthors();
            if (authors == null || authors.isEmpty()) {
                return null;
            }
            var icons = ContainerUtil.map(authors, author ->
                avatarIconsProvider.getIcon(author, CodeReviewChatItemUIUtil.ComponentType.COMPACT.getIconSize())
            );
            return new OverlaidOffsetIconsIcon(icons);
        }));

        ActionLink repliesLink = new ActionLink("", e -> vm.unfoldReplies());
        repliesLink.setAutoHideOnDisable(false);
        repliesLink.setFocusPainted(false);
        SwingBindingsKt.bindVisibilityIn(
            repliesLink,
            cs,
            FlowKt.combine(
                vm.getRepliesState(),
                vm.getCanCreateReplies(),
                (state, canReply) -> state.getRepliesCount() > 0 || canReply
            )
        );
        SwingBindingsKt.bindDisabledIn(repliesLink, cs, vm.getIsBusy());
        SwingBindingsKt.bindTextIn(
            repliesLink,
            cs,
            FlowKt.map(
                vm.getRepliesState(),
                state -> {
                    if (state.getRepliesCount() == 0) {
                        return CollaborationToolsLocalize.reviewCommentsReplyAction().get();
                    }
                    else {
                        return CollaborationToolsBundle.message("review.comments.replies.action", state.getRepliesCount());
                    }
                }
            )
        );

        JLabel lastReplyDateLabel = new JLabel();
        lastReplyDateLabel.setForeground(UIUtil.getContextHelpForeground());
        SwingBindingsKt.bindVisibilityIn(
            lastReplyDateLabel,
            cs,
            FlowKt.map(vm.getRepliesState(), state -> state.getLastReplyDate() != null)
        );
        SwingBindingsKt.bindTextIn(
            lastReplyDateLabel,
            cs,
            FlowKt.map(
                vm.getRepliesState(),
                state -> {
                    var date = state.getLastReplyDate();
                    return date != null ? DateFormatUtil.formatPrettyDateTime(date) : "";
                }
            )
        );

        HorizontalListPanel repliesActions = new HorizontalListPanel(
            CodeReviewTimelineUIUtil.Thread.Replies.ActionsFolded.HORIZONTAL_GAP
        );
        repliesActions.add(authorsLabel);
        repliesActions.add(repliesLink);
        repliesActions.add(lastReplyDateLabel);
        CollaborationToolsUIUtil.hideWhenNoVisibleChildren(repliesActions);

        HorizontalListPanel panel = new HorizontalListPanel(
            CodeReviewTimelineUIUtil.Thread.Replies.ActionsFolded.HORIZONTAL_GROUP_GAP
        );
        panel.setBorder(JBUI.Borders.empty(CodeReviewTimelineUIUtil.Thread.Replies.ActionsFolded.VERTICAL_PADDING, 0));
        panel.add(repliesActions);
        CollaborationToolsUIUtil.hideWhenNoVisibleChildren(panel);

        if (vm instanceof CodeReviewResolvableItemViewModel resolvableVm) {
            ActionLink unResolveLink = new ActionLink("", e -> resolvableVm.changeResolvedState());
            unResolveLink.setAutoHideOnDisable(false);
            unResolveLink.setFocusPainted(false);
            SwingBindingsKt.bindVisibilityIn(unResolveLink, cs, resolvableVm.getCanChangeResolvedState());
            SwingBindingsKt.bindDisabledIn(unResolveLink, cs, resolvableVm.getIsBusy());
            SwingBindingsKt.bindTextIn(
                unResolveLink,
                cs,
                FlowKt.map(resolvableVm.getIsResolved(), resolved -> getResolveToggleActionText(resolved))
            );
            panel.add(unResolveLink);
        }
        return panel;
    }

    public static @Nonnull String getResolveToggleActionText(boolean resolved) {
        return resolved
            ? CollaborationToolsLocalize.reviewCommentsUnresolveAction().get()
            : CollaborationToolsLocalize.reviewCommentsResolveAction().get();
    }

    public static final class Title {
        public static final int HORIZONTAL_GAP = 8;
        public static final int GROUP_HORIZONTAL_GAP = 12;

        private Title() {
        }
    }

    public static final class Actions {
        public static final int HORIZONTAL_GAP = 8;

        private Actions() {
        }
    }
}
