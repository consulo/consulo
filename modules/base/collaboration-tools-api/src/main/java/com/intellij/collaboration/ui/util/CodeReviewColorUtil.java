// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.ColorUtil;
import jakarta.annotation.Nonnull;

public final class CodeReviewColorUtil {
    private CodeReviewColorUtil() {
    }

    public static final class Review {
        private Review() {
        }

        public static final @Nonnull JBColor stateForeground =
            CollaborationToolsUIUtil.jbColorFromHex("Review.State.Foreground", "#6C707E", "#868A91");

        public static final @Nonnull JBColor stateBackground =
            CollaborationToolsUIUtil.jbColorFromHex("Review.State.Background", "#DFE1E5", "#43454A");

        public static final class Chat {
            private Chat() {
            }

            public static final @Nonnull JBColor hover =
                CollaborationToolsUIUtil.jbColorFromHex("Review.ChatItem.Hover", "#DFE1E533", "#5A5D6333");
        }

        public static final class LineFrame {
            private LineFrame() {
            }

            public static final @Nonnull JBColor border =
                JBColor.namedColor("Review.LineFrame.BorderColor", new JBColor(0x3574F0, 0x548AF7));
        }
    }

    public static final class Branch {
        private Branch() {
        }

        public static final @Nonnull JBColor background =
            JBColor.namedColor("Review.Branch.Background", ColorUtil.fromHex("#EBECF0"));

        public static final @Nonnull JBColor backgroundHovered =
            JBColor.namedColor("Review.Branch.Background.Hover", ColorUtil.fromHex("#DFE1E5"));
    }

    public static final class Reaction {
        private Reaction() {
        }

        public static final @Nonnull JBColor background =
            JBColor.namedColor("Review.Reaction.Background", new JBColor(0xEBECF0, 0x2B2D30));

        public static final @Nonnull JBColor backgroundHovered =
            JBColor.namedColor("Review.Reaction.Background.Hovered", new JBColor(0xDFE1E5, 0x393B40));

        public static final @Nonnull JBColor backgroundPressed =
            JBColor.namedColor("Review.Reaction.Background.Pressed", new JBColor(0xEDF3FF, 0x25324D));

        // TODO: provide correct color
        public static final @Nonnull JBColor backgroundReacted = background;

        public static final @Nonnull JBColor borderReacted =
            JBColor.namedColor("Review.Reaction.Border.Reacted", new JBColor(0x3574F0, 0x548AF7));
    }

    public static final class AI {
        private AI() {
        }

        public static final @Nonnull JBColor background =
            JBColor.namedColor("Review.AI.Background", new JBColor(0x834DF0, 0x834DF0));
    }
}
