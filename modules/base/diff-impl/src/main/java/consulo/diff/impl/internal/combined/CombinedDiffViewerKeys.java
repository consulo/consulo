// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.util.dataholder.Key;

public final class CombinedDiffViewerKeys {
    public static final Key<CombinedDiffViewer> COMBINED_DIFF_VIEWER = Key.create("combined_diff_viewer_data");
    public static final Key<CombinedDiffViewer> COMBINED_DIFF_VIEWER_KEY = Key.create("combined_diff_viewer");
    public static final Key<CombinedDiffMainUI> COMBINED_DIFF_MAIN_UI = Key.create("combined_diff_main_ui");

    private CombinedDiffViewerKeys() {
    }
}
