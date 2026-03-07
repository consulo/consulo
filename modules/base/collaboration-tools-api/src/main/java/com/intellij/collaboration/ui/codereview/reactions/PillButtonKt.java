// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.reactions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

final class PillButtonKt {
    private PillButtonKt() {
    }

    static void setBorderColor(@Nonnull PillButton button, @Nullable Color color) {
        button.setBorder(new PillButtonBorder(color));
    }

    static void setBorderColor(@Nonnull PillButton button) {
        setBorderColor(button, null);
    }
}
