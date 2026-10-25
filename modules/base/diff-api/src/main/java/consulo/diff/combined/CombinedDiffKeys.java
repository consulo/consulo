// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.util.dataholder.Key;

public final class CombinedDiffKeys {
    public static final Key<CombinedBlockId> COMBINED_DIFF_SCROLL_TO_BLOCK = Key.create("combined_diff_scroll_to_block");
    public static final Key<Boolean> COMBINED_DIFF_VIEWER_INITIAL_FOCUS_REQUEST = Key.create("combined_diff_viewer_initial_focus_request");
    public static final Key<CombinedDiffToggle> COMBINED_DIFF_TOGGLE = Key.create("Diff.CombinedDiffToggle");
    public static final Key<Boolean> DISABLE_LOADING_BLOCKS = Key.create("combined_diff_viewer_dont_show_loading_blocks");

    private CombinedDiffKeys() {
    }
}
