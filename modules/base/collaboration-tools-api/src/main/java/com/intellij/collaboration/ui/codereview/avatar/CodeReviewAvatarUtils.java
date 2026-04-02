// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.avatar;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public final class CodeReviewAvatarUtils {
    static final int INNER_WIDTH = 1;
    static final int OUTLINE_WIDTH = 2;

    private CodeReviewAvatarUtils() {
    }

    public static int expectedIconHeight() {
        return expectedIconHeight(Avatar.Sizes.OUTLINED);
    }

    public static int expectedIconHeight(int size) {
        return size + 2 * (INNER_WIDTH + OUTLINE_WIDTH);
    }

    public static @Nonnull Icon createIconWithOutline(@Nonnull Icon avatarIcon, @Nonnull Color outlineColor) {
        return new AvatarIconWithOutline(avatarIcon, outlineColor);
    }
}
