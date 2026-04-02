// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.avatar;

import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;

public final class Avatar {
    private Avatar() {
    }

    /**
     * Avatar sizes in different collaboration UIs
     */
    public static final class Sizes {
        private Sizes() {
        }

        /**
         * Usages:
         * Mentions in comments
         */
        public static final int SMALL = 15;

        /**
         * Usages:
         * 1. Reviewer's selector
         * 2. Replies
         */
        public static final int BASE = 20;

        /**
         * Usages:
         * 1. Code reviews list
         * 2. Details
         */
        public static final int OUTLINED = 18;

        /**
         * Usages:
         * Top level comment in timeline
         */
        public static final int TIMELINE = 30;

        /**
         * Usages:
         * Account representation in settings and popups
         */
        public static final int ACCOUNT = 40;
    }

    public static final class Color {
        private Color() {
        }

        public static final @Nonnull JBColor ACCEPTED_BORDER =
            JBColor.namedColor("Review.Avatar.Border.Status.Accepted", new JBColor(0x5FB865, 0x57965C));
        public static final @Nonnull JBColor WAIT_FOR_UPDATES_BORDER =
            JBColor.namedColor("Review.Avatar.Border.Status.WaitForUpdates", new JBColor(0xEC8F4C, 0xE08855));
        public static final @Nonnull JBColor NEED_REVIEW_BORDER =
            JBColor.namedColor("Review.Avatar.Border.Status.NeedReview", new JBColor(0x818594, 0x6F737A));
    }
}
