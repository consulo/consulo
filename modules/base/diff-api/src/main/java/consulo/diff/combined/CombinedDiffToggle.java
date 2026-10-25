// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.diff.internal.DiffSettingsHolder;

public interface CombinedDiffToggle {
    CombinedDiffToggle DEFAULT = new CombinedDiffToggle() {
        @Override
        public boolean isCombinedDiffEnabled() {
            return DiffSettingsHolder.DiffSettings.getSettings().isCombinedDiffEnabled();
        }

        @Override
        public void setCombinedDiffEnabled(boolean enabled) {
            DiffSettingsHolder.DiffSettings.getSettings().setCombinedDiffEnabled(enabled);
        }
    };

    boolean isCombinedDiffEnabled();

    void setCombinedDiffEnabled(boolean enabled);
}
