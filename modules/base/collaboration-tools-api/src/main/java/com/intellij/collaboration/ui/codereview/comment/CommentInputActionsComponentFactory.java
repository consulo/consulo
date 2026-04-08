package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.HorizontalListPanel;
import com.intellij.collaboration.ui.VerticalListPanel;
import com.intellij.collaboration.ui.util.ActivatableCoroutineScopeProvider;
import com.intellij.collaboration.ui.util.SwingActionUtilKt;
import com.intellij.collaboration.ui.util.SwingBindingsKt;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class CommentInputActionsComponentFactory {
    private CommentInputActionsComponentFactory() {
    }

    public static @Nonnull String getSubmitShortcutText() {
        return KeymapUtil.getFirstKeyboardShortcutText(getSubmitShortcut());
    }

    public static @Nonnull String getNewLineShortcutText() {
        return KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.ENTER);
    }

    private static @Nonnull ShortcutSet getSubmitShortcut() {
        return CommonShortcuts.getCtrlEnter();
    }

    private static final ShortcutSet CANCEL_SHORTCUT = CommonShortcuts.ESCAPE;

    public static @Nonnull JComponent create(@Nonnull CoroutineScope cs, @Nonnull Config cfg) {
        JPanel panel = new JPanel(null);
        panel.setOpaque(false);
        panel.setLayout(new net.miginfocom.swing.MigLayout(
            new net.miginfocom.layout.LC().insets("0").gridGap("12", "0").fill().noGrid()));

        panel.add(
            createHintsComponent(cs, cfg.submitHint),
            new net.miginfocom.layout.CC().minWidth("0").shrinkPrio(10).alignX("right")
        );
        panel.add(
            createActionButtonsComponent(cs, cfg),
            new net.miginfocom.layout.CC().shrinkPrio(0).alignX("right")
        );
        return panel;
    }

    private static @Nonnull JComponent createHintsComponent(
        @Nonnull CoroutineScope cs,
        @Nonnull StateFlow<@Nls String> submitHintState
    ) {
        HorizontalListPanel panel = new HorizontalListPanel(12);
        panel.add(createHintLabel(
            CollaborationToolsLocalize.reviewCommentNewLineHint(getNewLineShortcutText()).get()
        ));

        SwingBindingsKt.bindChildIn(panel, cs, submitHintState, 0, hint -> createHintLabel(hint));
        return panel;
    }

    private static @Nonnull JLabel createHintLabel(@Nls @Nonnull String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setFont(JBFont.small());
        label.setMinimumSize(new Dimension(0, 0));
        return label;
    }

    private static @Nonnull JComponent createActionButtonsComponent(@Nonnull CoroutineScope cs, @Nonnull Config cfg) {
        HorizontalListPanel panel = new HorizontalListPanel(8);
        var buttonsFlow = FlowKt.combine(
            cfg.primaryAction,
            cfg.secondaryActions,
            cfg.additionalActions,
            cfg.cancelAction,
            (primary, secondary, additional, cancel) -> {
                List<JComponent> buttons = new ArrayList<>();
                if (cancel != null) {
                    String name = SwingActionUtilKt.getName(cancel);
                    if (name != null && !name.isEmpty()) {
                        JButton cancelButton = new JButton(cancel);
                        cancelButton.setOpaque(false);
                        buttons.add(cancelButton);
                    }
                }

                for (Action additionalAction : additional) {
                    JButton btn = new JButton(additionalAction);
                    btn.setOpaque(false);
                    buttons.add(btn);
                }

                JBOptionButton optionButton = new JBOptionButton(primary, secondary.toArray(new Action[0]));
                CollaborationToolsUIUtil.setDefault(optionButton, true);
                buttons.add(optionButton);

                return buttons;
            }
        );

        kotlinx.coroutines.CoroutineScopeKt.launch(
            cs,
            null,
            null,
            (scope, continuation) -> {
                buttonsFlow.collect(
                    buttons -> {
                        panel.removeAll();
                        for (JComponent button : buttons) {
                            panel.add(button);
                        }
                        panel.revalidate();
                        panel.repaint();
                        return null;
                    },
                    continuation
                );
                return null;
            }
        );

        return panel;
    }

    public static final class Config {
        public final @Nonnull StateFlow<Action> primaryAction;
        public final @Nonnull StateFlow<List<Action>> secondaryActions;
        public final @Nonnull StateFlow<List<Action>> additionalActions;
        public final @Nonnull StateFlow<Action> cancelAction;
        public final @Nonnull StateFlow<@Nls String> submitHint;

        public Config(
            @Nonnull StateFlow<Action> primaryAction,
            @Nonnull StateFlow<List<Action>> secondaryActions,
            @Nonnull StateFlow<List<Action>> additionalActions,
            @Nonnull StateFlow<Action> cancelAction,
            @Nonnull StateFlow<@Nls String> submitHint
        ) {
            this.primaryAction = primaryAction;
            this.secondaryActions = secondaryActions;
            this.additionalActions = additionalActions;
            this.cancelAction = cancelAction;
            this.submitHint = submitHint;
        }

        public Config(
            @Nonnull StateFlow<Action> primaryAction,
            @Nonnull StateFlow<@Nls String> submitHint
        ) {
            this(
                primaryAction,
                StateFlowKt.MutableStateFlow(List.of()),
                StateFlowKt.MutableStateFlow(List.of()),
                StateFlowKt.MutableStateFlow(null),
                submitHint
            );
        }
    }

    /**
     * @deprecated Use a version with CoroutineScope
     */
    @Deprecated
    public static @Nonnull JComponent attachActions(@Nonnull JComponent component, @Nonnull Config cfg) {
        VerticalListPanel panel = new VerticalListPanel();
        panel.add(component);
        ActivatableCoroutineScopeProvider provider = new ActivatableCoroutineScopeProvider();
        provider.launchInScope(cs -> {
            JComponent actionsPanel = create(cs, cfg);
            panel.add(actionsPanel);
            panel.validate();
            panel.repaint();

            installActionShortcut(panel, cs, cfg.primaryAction, getSubmitShortcut(), true);
            installActionShortcut(panel, cs, cfg.cancelAction, CANCEL_SHORTCUT, false);

            try {
                kotlinx.coroutines.AwaitCancellationKt.awaitCancellation();
            }
            finally {
                panel.remove(actionsPanel);
                panel.revalidate();
                panel.repaint();
            }
            return null;
        });
        provider.activateWith(panel);
        return panel;
    }

    public static @Nonnull JComponent attachActions(
        @Nonnull CoroutineScope cs,
        @Nonnull JComponent component,
        @Nonnull Config cfg
    ) {
        VerticalListPanel panel = new VerticalListPanel();
        panel.add(component);
        panel.add(create(cs, cfg));

        // override action bound to Ctrl+Enter because convenience wins
        installActionShortcut(panel, cs, cfg.primaryAction, getSubmitShortcut(), true);
        installActionShortcut(panel, cs, cfg.cancelAction, CANCEL_SHORTCUT, false);
        return panel;
    }

    private static void installActionShortcut(
        @Nonnull JComponent component,
        @Nonnull CoroutineScope cs,
        @Nonnull StateFlow<Action> action,
        @Nonnull ShortcutSet shortcut,
        boolean overrideEditorAction
    ) {
        kotlinx.coroutines.CoroutineScopeKt.launch(
            cs,
            null,
            null,
            (scope, continuation) -> {
                FlowKt.filterNotNull(action).collectLatest(
                    (act, cont) -> {
                        AnAction anAction = overrideEditorAction
                            ? SwingActionUtilKt.toAnAction(act)
                            : toAnActionWithEditorPromotion(act);
                        try {
                            anAction.registerCustomShortcutSet(shortcut, component);
                            kotlinx.coroutines.AwaitCancellationKt.awaitCancellation();
                        }
                        finally {
                            anAction.unregisterCustomShortcutSet(component);
                        }
                        return null;
                    },
                    continuation
                );
                return null;
            }
        );
    }

    /**
     * This is required for Esc handler to work properly (close popup or clear selection).
     * Otherwise our Esc handler will fire first.
     */
    private static @Nonnull AnAction toAnActionWithEditorPromotion(@Nonnull Action action) {
        String name = SwingActionUtilKt.getName(action);
        return new DumbAwareAction(name != null ? name : "") {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(action.isEnabled());
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent event) {
                SwingActionUtilKt.performAction(action, event);
            }

            // ActionPromoter functionality - promote editor actions
        };
    }
}
