// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import jakarta.annotation.Nonnull;

public final class CodeReviewAdvancedSettings {
    static final String COMBINED_DIFF_SETTING_ID = "enable.combined.diff.for.codereview";

    private CodeReviewAdvancedSettings() {
    }

    public static boolean isCombinedDiffEnabled() {
        return AdvancedSettings.getBoolean(COMBINED_DIFF_SETTING_ID) && !AppMode.isRemoteDevHost();
    }

    private static void setCombinedDiffEnabled(boolean enabled) {
        AdvancedSettings.setBoolean(COMBINED_DIFF_SETTING_ID, enabled);
    }

    public static final @Nonnull CombinedDiffToggle CodeReviewCombinedDiffToggle = new CombinedDiffToggle() {
        @Override
        public boolean isCombinedDiffEnabled() {
            return CodeReviewAdvancedSettings.isCombinedDiffEnabled();
        }

        @Override
        public void setCombinedDiffEnabled(boolean value) {
            CodeReviewAdvancedSettings.setCombinedDiffEnabled(value);
        }
    };
}
