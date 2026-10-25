// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.ui;

import consulo.ui.color.ColorValue;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;

public final class BranchPresentation {
    public static final ColorValue TEXT_COLOR = ComponentColors.VCS_REF_LABEL_FOREGROUND;

    private static final ColorValue BACKGROUND_BASE_COLOR = ComponentColors.VCS_REF_LABEL_BACKGROUND_BASE;

    private static final double BACKGROUND_BALANCE = 0.08;

    private BranchPresentation() {
    }

    public static ColorValue getBranchPresentationBackground(ColorValue background) {
        return ColorValueUtil.mix(background, BACKGROUND_BASE_COLOR, BACKGROUND_BALANCE);
    }
}
