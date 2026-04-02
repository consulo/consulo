// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.JPanelWithBackground;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.avatar.Avatar;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil;
import com.intellij.collaboration.ui.util.CodeReviewColorUtil;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.event.HoverStateListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.ui.ex.awt.JBUI.Panels.simplePanel;

public final class CodeReviewChatItemUIUtil {

    private CodeReviewChatItemUIUtil() {
    }

    /**
     * Maximum width for textual content for it to be readable.
     * Equals to 42em.
     */
    public static int getTextContentWidth() {
        return Math.round(JBUIScale.DEF_SYSTEM_FONT_SIZE * 42);
    }

    public static final int THREAD_TOP_MARGIN = 8;

    @SuppressWarnings("UseDPIAwareInsets")
    public enum ComponentType {
        /**
         * Full-sized component, to be used in timeline
         */
        FULL(Avatar.Sizes.TIMELINE, 14,
            new Insets(
                CodeReviewTimelineUIUtil.ITEM_VERT_PADDING,
                CodeReviewTimelineUIUtil.ITEM_HOR_PADDING,
                CodeReviewTimelineUIUtil.ITEM_VERT_PADDING,
                CodeReviewTimelineUIUtil.ITEM_HOR_PADDING
            )
        ) {
            @Override
            public @Nonnull Insets getInputPaddingInsets() {
                return getPaddingInsets();
            }
        },

        /**
         * Special horizontally shifted compact-sized component, to be used in second level of timeline
         */
        FULL_SECONDARY(Avatar.Sizes.BASE, 10,
            new Insets(4, FULL.getFullLeftShift(), 4, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
        ) {
            @Override
            public @Nonnull Insets getInputPaddingInsets() {
                return new Insets(6, FULL.getFullLeftShift(), 6, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING);
            }
        },

        /**
         * Compact-sized component to be used in diffs and other places where space is scarce
         */
        COMPACT(Avatar.Sizes.BASE, 10,
            new Insets(4, CodeReviewCommentUIUtil.INLAY_PADDING, 4, CodeReviewCommentUIUtil.INLAY_PADDING)
        ) {
            @Override
            public @Nonnull Insets getInputPaddingInsets() {
                return new Insets(6, CodeReviewCommentUIUtil.INLAY_PADDING, 6, CodeReviewCommentUIUtil.INLAY_PADDING);
            }
        },

        /**
         * Same as {@link #COMPACT} but without any padding at all
         */
        SUPER_COMPACT(Avatar.Sizes.BASE, 10, new Insets(0, 0, 0, 0)) {
            @Override
            public @Nonnull Insets getInputPaddingInsets() {
                return new Insets(0, 0, 0, 0);
            }
        };

        private final int iconSize;
        private final int iconGap;
        private final Insets paddingInsets;

        ComponentType(int iconSize, int iconGap, Insets paddingInsets) {
            this.iconSize = iconSize;
            this.iconGap = iconGap;
            this.paddingInsets = paddingInsets;
        }

        /**
         * Size of a component icon
         */
        public int getIconSize() {
            return iconSize;
        }

        /**
         * Gap between icon and component body
         */
        public int getIconGap() {
            return iconGap;
        }

        /**
         * Component padding that is included in hover
         */
        public @Nonnull Insets getPaddingInsets() {
            return paddingInsets;
        }

        /**
         * Padding for the input component related to the item
         */
        public abstract @Nonnull Insets getInputPaddingInsets();

        /**
         * Item body shift from the left side
         */
        public int getFullLeftShift() {
            return paddingInsets.left + iconSize + iconGap;
        }

        /**
         * Item body shift from the left side without padding
         */
        public int getContentLeftShift() {
            return iconSize + iconGap;
        }
    }

    public static @Nonnull JComponent build(
        @Nonnull ComponentType type,
        @Nonnull Function<Integer, Icon> iconProvider,
        @Nonnull JComponent content,
        @Nonnull Consumer<Builder> init
    ) {
        return buildDynamic(type, iconSize -> new SingleValueModel<>(iconProvider.apply(iconSize)), content, init);
    }

    public static @Nonnull JComponent buildDynamic(
        @Nonnull ComponentType type,
        @Nonnull Function<Integer, SingleValueModel<Icon>> iconValueProvider,
        @Nonnull JComponent content,
        @Nonnull Consumer<Builder> init
    ) {
        Builder builder = new Builder(type, iconValueProvider, content);
        init.accept(builder);
        return builder.build();
    }

    /**
     * One-time use builder, be careful not to leak {@link #header}
     */
    public static final class Builder {
        private final ComponentType type;
        private final Function<Integer, SingleValueModel<Icon>> iconValueProvider;
        private final JComponent content;

        /**
         * Tooltip for a main icon
         */
        public @Nullable
        @Nls String iconTooltip = null;

        /**
         * Content width limit
         */
        public @Nullable Integer maxContentWidth = getTextContentWidth();

        /**
         * Header components - title and actions.
         * Actions component will only be visible on item hover.
         */
        public @Nullable HeaderComponents header = null;

        /**
         * The color to use as the background color on-hover.
         */
        public @Nonnull JBColor hoverHighlight = CodeReviewColorUtil.Review.Chat.hover;

        public Builder(
            @Nonnull ComponentType type,
            @Nonnull Function<Integer, SingleValueModel<Icon>> iconValueProvider,
            @Nonnull JComponent content
        ) {
            this.type = type;
            this.iconValueProvider = iconValueProvider;
            this.content = content;
        }

        /**
         * Helper method to setup {@link HeaderComponents}
         */
        public @Nonnull Builder withHeader(@Nonnull JComponent title, @Nullable JComponent actions) {
            header = new HeaderComponents(title, actions);
            return this;
        }

        public @Nonnull Builder withHeader(@Nonnull JComponent title) {
            return withHeader(title, null);
        }

        public @Nonnull JComponent build() {
            JComponent result = content;

            if (maxContentWidth != null) {
                result = CollaborationToolsUIUtil.wrapWithLimitedSize(result, maxContentWidth);
            }

            if (header != null) {
                result = ComponentFactory.wrapWithHeader(result, header.title(), header.actions());
            }

            JLabel iconLabel = new JLabel();
            iconLabel.setToolTipText(iconTooltip);
            iconLabel.setBorder(JBUI.Borders.emptyRight(type.getIconGap()));
            iconValueProvider.apply(type.getIconSize()).addAndInvokeListener(iconLabel::setIcon);

            JPanel iconPanel = simplePanel().addToTop(iconLabel).andTransparent();
            JComponent withIcon = simplePanel(result).addToLeft(iconPanel).andTransparent();

            JComponent actionsComp = header != null ? header.actions() : null;
            actionsVisibleOnHover(withIcon, actionsComp);

            withIcon.setBorder(JBUI.Borders.empty(type.getPaddingInsets()));

            return withHoverHighlight(withIcon, hoverHighlight);
        }
    }

    public record HeaderComponents(@Nonnull JComponent title, @Nullable JComponent actions) {
    }

    // TODO: custom layouts
    public static final class ComponentFactory {
        private ComponentFactory() {
        }

        public static @Nonnull JComponent wrapWithHeader(
            @Nonnull JComponent item,
            @Nonnull JComponent title,
            @Nullable JComponent actions
        ) {
            JPanel headerPanel = new JPanel(null);
            headerPanel.setLayout(new net.miginfocom.swing.MigLayout(
                new net.miginfocom.layout.LC().gridGap("0", "0").insets("0").height("16")
                    .hideMode(net.miginfocom.layout.HideMode.DISREGARD).fill()));
            headerPanel.setOpaque(false);

            headerPanel.add(title, new net.miginfocom.layout.CC().push());
            if (actions != null) {
                headerPanel.add(
                    actions,
                    new net.miginfocom.layout.CC().push().gapLeft("10:push").hideMode(net.miginfocom.layout.HideMode.NORMAL.code)
                );
            }

            VerticalListPanel panel = new VerticalListPanel(4);
            panel.add(headerPanel);
            panel.add(item);
            return panel;
        }
    }

    public static void actionsVisibleOnHover(@Nonnull JComponent comp, @Nullable JComponent actionsPanel) {
        if (actionsPanel != null) {
            HoverStateListener listener = new HoverStateListener() {
                @Override
                protected void hoverChanged(@Nonnull Component component, boolean hovered) {
                    actionsPanel.setVisible(hovered);
                }
            };
            // reset hover to false
            listener.mouseExited(comp);
            listener.addTo(comp);
        }
    }

    public static @Nonnull JComponent withHoverHighlight(@Nonnull JComponent comp, @Nonnull JBColor hoverHighlight) {
        JPanelWithBackground highlighterPanel = new JPanelWithBackground(new BorderLayout());
        highlighterPanel.setOpaque(false);
        highlighterPanel.setBackground(null);
        highlighterPanel.add(comp, BorderLayout.CENTER);

        HoverStateListener listener = new HoverStateListener() {
            @Override
            protected void hoverChanged(@Nonnull Component component, boolean hovered) {
                // TODO: extract to theme colors
                component.setBackground(hovered ? hoverHighlight : null);
            }
        };
        // reset hover to false
        listener.mouseExited(highlighterPanel);
        listener.addTo(highlighterPanel);

        return highlighterPanel;
    }
}
