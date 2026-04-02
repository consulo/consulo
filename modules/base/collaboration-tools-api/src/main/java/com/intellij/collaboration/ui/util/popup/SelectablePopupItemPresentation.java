// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util.popup;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Objects;

public interface SelectablePopupItemPresentation {
    @Nullable
    Icon getIcon();

    @Nls
    @Nonnull
    String getShortText();

    @Nls
    @Nullable
    String getFullText();

    boolean isSelected();

    final class Simple implements SelectablePopupItemPresentation {
        private final @Nonnull String shortText;
        private final @Nullable Icon icon;
        private final @Nullable String fullText;
        private final boolean selected;

        public Simple(@Nonnull String shortText) {
            this(shortText, null, null, false);
        }

        public Simple(@Nonnull String shortText, @Nullable Icon icon) {
            this(shortText, icon, null, false);
        }

        public Simple(@Nonnull String shortText, @Nullable Icon icon, @Nullable String fullText) {
            this(shortText, icon, fullText, false);
        }

        public Simple(@Nonnull String shortText, @Nullable Icon icon, @Nullable String fullText, boolean selected) {
            this.shortText = shortText;
            this.icon = icon;
            this.fullText = fullText;
            this.selected = selected;
        }

        @Override
        public @Nullable Icon getIcon() {
            return icon;
        }

        @Override
        public @Nonnull String getShortText() {
            return shortText;
        }

        @Override
        public @Nullable String getFullText() {
            return fullText;
        }

        @Override
        public boolean isSelected() {
            return selected;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Simple s)) {
                return false;
            }
            return selected == s.selected && Objects.equals(shortText, s.shortText) &&
                Objects.equals(icon, s.icon) && Objects.equals(fullText, s.fullText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shortText, icon, fullText, selected);
        }

        @Override
        public String toString() {
            return "Simple(shortText=" + shortText + ", icon=" + icon + ", fullText=" + fullText + ", isSelected=" + selected + ")";
        }
    }
}
