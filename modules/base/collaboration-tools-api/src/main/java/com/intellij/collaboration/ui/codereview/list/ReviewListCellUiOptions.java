// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ReviewListCellUiOptions {
    private final boolean bordered;

    public ReviewListCellUiOptions(boolean bordered) {
        this.bordered = bordered;
    }

    public ReviewListCellUiOptions() {
        this(true);
    }

    public boolean isBordered() {
        return bordered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReviewListCellUiOptions that = (ReviewListCellUiOptions) o;
        return bordered == that.bordered;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bordered);
    }

    @Override
    public String toString() {
        return "ReviewListCellUiOptions(bordered=" + bordered + ")";
    }
}
