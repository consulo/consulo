// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import jakarta.annotation.Nonnull;

import java.awt.*;

@ApiStatus.Internal
public final class MigLayoutUtil {
    private MigLayoutUtil() {
    }

    public static @Nonnull CC gap(@Nonnull CC cc, int left, int right, int top, int bottom) {
        return cc.gap(String.valueOf(left), String.valueOf(right), String.valueOf(top), String.valueOf(bottom));
    }

    public static @Nonnull CC gap(@Nonnull CC cc) {
        return gap(cc, 0, 0, 0, 0);
    }

    public static @Nonnull CC gap(@Nonnull CC cc, @Nonnull Insets insets) {
        return gap(cc, insets.left, insets.right, insets.top, insets.bottom);
    }

    public static @Nonnull LC emptyBorders(@Nonnull LC lc) {
        return lc.gridGap("0", "0").insets("0", "0", "0", "0");
    }
}
