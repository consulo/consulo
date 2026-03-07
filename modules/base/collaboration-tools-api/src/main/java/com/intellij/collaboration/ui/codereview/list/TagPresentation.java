// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Objects;

public interface TagPresentation {
    @Nonnull
    @Nls
    String getName();

    @Nullable
    Color getColor();

    final class Simple implements TagPresentation {
        private final @Nonnull String name;
        private final @Nullable Color color;

        public Simple(@Nonnull String name, @Nullable Color color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public @Nonnull String getName() {
            return name;
        }

        @Override
        public @Nullable Color getColor() {
            return color;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Simple simple = (Simple) o;
            return Objects.equals(name, simple.name) && Objects.equals(color, simple.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, color);
        }

        @Override
        public String toString() {
            return "Simple(name=" + name + ", color=" + color + ")";
        }
    }
}
