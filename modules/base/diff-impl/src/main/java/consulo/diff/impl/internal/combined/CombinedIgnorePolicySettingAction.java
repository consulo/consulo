// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;

class CombinedIgnorePolicySettingAction extends TextDiffViewerUtil.IgnorePolicySettingAction {
    private final Runnable myRediff;

    CombinedIgnorePolicySettingAction(TextDiffSettingsHolder.TextDiffSettings settings, Runnable rediff) {
        super(settings);
        myRediff = rediff;
    }

    @Override
    protected void onSettingsChanged() {
        myRediff.run();
    }
}
