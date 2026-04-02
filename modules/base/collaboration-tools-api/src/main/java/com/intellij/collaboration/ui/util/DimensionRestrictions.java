// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public interface DimensionRestrictions {
    @Nullable
    Integer getWidth();

    @Nullable
    Integer getHeight();

    final class ScalingConstant implements DimensionRestrictions {
        private final @Nullable Integer width;
        private final @Nullable Integer height;

        public ScalingConstant() {
            this(null, null);
        }

        public ScalingConstant(@Nullable Integer width, @Nullable Integer height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public @Nullable Integer getWidth() {
            return width != null ? JBUIScale.scale(width) : null;
        }

        @Override
        public @Nullable Integer getHeight() {
            return height != null ? JBUIScale.scale(height) : null;
        }
    }

    final class LinesHeight implements DimensionRestrictions {
        private final @Nonnull JComponent component;
        private final int linesCount;
        private final @Nullable Integer scalableWidth;

        public LinesHeight(@Nonnull JComponent component, int linesCount) {
            this(component, linesCount, null);
        }

        public LinesHeight(@Nonnull JComponent component, int linesCount, @Nullable Integer scalableWidth) {
            this.component = component;
            this.linesCount = linesCount;
            this.scalableWidth = scalableWidth;
        }

        @Override
        public @Nullable Integer getWidth() {
            return scalableWidth != null ? JBUIScale.scale(scalableWidth) : null;
        }

        @Override
        public @Nonnull Integer getHeight() {
            return UIUtil.getLineHeight(component) * linesCount;
        }
    }

    DimensionRestrictions None = new DimensionRestrictions() {
        @Override
        public @Nullable Integer getWidth() {
            return null;
        }

        @Override
        public @Nullable Integer getHeight() {
            return null;
        }
    };
}
