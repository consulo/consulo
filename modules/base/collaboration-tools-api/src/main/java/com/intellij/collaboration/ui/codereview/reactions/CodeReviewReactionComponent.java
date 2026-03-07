// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import com.intellij.collaboration.ui.util.CodeReviewColorUtil;
import consulo.ui.ex.awt.JBFont;
import consulo.util.concurrent.AsyncUtil;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import java.util.function.Consumer;

public final class CodeReviewReactionComponent {
    private CodeReviewReactionComponent() {
    }

    public static @Nonnull JComponent createReactionButtonIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<CodeReviewReactionPillPresentation> presentation,
        @Nonnull Runnable toggle
    ) {
        PillButton button = new PillButton();
        button.setFocusable(false);
        button.setFont(JBFont.create(button.getFont().deriveFont(CodeReviewReactionsUIUtil.COUNTER_FONT_SIZE)));

        AsyncUtil.launchNow(
            cs,
            continuation -> {
                FlowKt.distinctUntilChanged(presentation).collect(
                    it -> {
                        button.setIcon(it.getIcon(CodeReviewReactionsUIUtil.ICON_SIZE));
                        button.setText(String.valueOf(it.getReactors().size()));
                        if (it.isOwnReaction()) {
                            PillButtonKt.setBorderColor(button, CodeReviewColorUtil.Reaction.borderReacted);
                            button.setBackground(CodeReviewColorUtil.Reaction.backgroundReacted);
                        }
                        else {
                            PillButtonKt.setBorderColor(button, null);
                            button.setBackground(CodeReviewColorUtil.Reaction.background);
                        }
                        button.setToolTipText(CodeReviewReactionsUIUtil.createTooltipText(it.getReactors(), it.getReactionName()));
                        return null;
                    },
                    continuation
                );
                return null;
            }
        );

        button.addActionListener(e -> toggle.run());
        return button;
    }

    public static @Nonnull JComponent createNewReactionButton(@Nonnull Consumer<JComponent> showPicker) {
        PillButton button = new PillButton();
        button.setFocusable(false);
        button.setIcon(CollaborationToolsIcons.AddEmoji);
        button.setMargin(JBInsets.create(3, 6));

        button.addActionListener(e -> showPicker.accept(button));
        return button;
    }

    public static @Nonnull JComponent createPickReactionButton(@Nonnull Icon emojiIcon, @Nonnull Runnable pick) {
        PillButton button = new PillButton();
        button.setFocusable(false);
        button.setIcon(emojiIcon);
        button.addActionListener(e -> pick.run());
        return button;
    }
}
