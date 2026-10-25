// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.internal.DiffSettingsHolder;

public final class CombinedDiffRegistry {
    private CombinedDiffRegistry() {
    }

    public static boolean isEnabled() {
        return DiffSettingsHolder.DiffSettings.getSettings().isCombinedDiffEnabled();
    }

    public static void setCombinedDiffEnabled(boolean enabled) {
        DiffSettingsHolder.DiffSettings.getSettings().setCombinedDiffEnabled(enabled);
    }

    /**
     * Number of blocks which need to be loaded before and beyond the viewport.
     */
    public static int getPreloadedBlocksCount() {
        return 3;
    }

    /**
     * Max number of loaded files diff contents to keep. "-1" keeps contents only inside the viewport (+ the preloaded blocks).
     */
    public static int getMaxBlockCountInMemory() {
        return 20;
    }

    /**
     * If the limit is reached, the simple diff is shown instead of the combined one. "-1" turns the limit off.
     */
    public static int getFilesLimit() {
        return 1000;
    }
}
