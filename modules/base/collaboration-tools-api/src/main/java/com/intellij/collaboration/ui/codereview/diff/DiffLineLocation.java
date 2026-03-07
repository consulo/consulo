// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.diff.util.Side;
import kotlin.Pair;
import jakarta.annotation.Nonnull;

/**
 * Represents a location in a diff identified by a side and a line number.
 * Replacement for the Kotlin typealias {@code DiffLineLocation = Pair<Side, Int>}.
 */
public final class DiffLineLocation extends Pair<@Nonnull Side, @Nonnull Integer> {
    public DiffLineLocation(@Nonnull Side side, int line) {
        super(side, line);
    }

    public @Nonnull Side getSide() {
        return getFirst();
    }

    public int getLine() {
        return getSecond();
    }
}
