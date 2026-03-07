// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.timeline.comment;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.icon.IconsProvider;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.IntSupplier;

public final class CommentTextFieldFactory {
    private CommentTextFieldFactory() {
    }

    public sealed interface ScrollOnChangePolicy {
        final class DontScroll implements ScrollOnChangePolicy {
            public static final DontScroll INSTANCE = new DontScroll();

            private DontScroll() {
            }
        }

        final class ScrollToField implements ScrollOnChangePolicy {
            public static final ScrollToField INSTANCE = new ScrollToField();

            private ScrollToField() {
            }
        }

        final class ScrollToComponent implements ScrollOnChangePolicy {
            private final JComponent component;

            public ScrollToComponent(@Nonnull JComponent component) {
                this.component = component;
            }

            public @Nonnull JComponent getComponent() {
                return component;
            }
        }
    }

    static @Nonnull JComponent wrapWithLeftIcon(
        @Nonnull IconConfig config,
        @Nonnull JComponent item,
        @Nonnull IntSupplier minimalItemHeightCalculator
    ) {
        Icon icon = config.icon();
        int iconGap = config.gap();
        JLabel iconLabel = new JLabel(icon);
        JPanel panel = new JPanel(new CommentFieldWithIconLayout(
            iconGap - CollaborationToolsUIUtil.getFocusBorderInset(), minimalItemHeightCalculator));
        panel.setOpaque(false);
        panel.add(iconLabel, CommentFieldWithIconLayout.ICON);
        panel.add(item, CommentFieldWithIconLayout.ITEM);
        return panel;
    }

    public record IconConfig(@Nonnull Icon icon, int gap) {
        public static <T> @Nonnull IconConfig of(
            @Nonnull CodeReviewChatItemUIUtil.ComponentType type,
            @Nonnull IconsProvider<T> iconsProvider,
            @Nonnull T iconKey
        ) {
            return new IconConfig(iconsProvider.getIcon(iconKey, type.getIconSize()), type.getIconGap());
        }
    }
}
