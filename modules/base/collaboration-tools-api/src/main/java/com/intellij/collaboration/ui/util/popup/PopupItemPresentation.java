// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util.popup;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public interface PopupItemPresentation {
    @Nls
    @Nonnull
    String getShortText();

    @Nullable
    Icon getIcon();

    @Nls
    @Nullable
    String getFullText();

    final class Simple implements PopupItemPresentation {
        private final @Nonnull String shortText;
        private final @Nullable Icon icon;
        private final @Nullable String fullText;

        public Simple(@Nonnull String shortText) {
            this(shortText, null, null);
        }

        public Simple(@Nonnull String shortText, @Nullable Icon icon) {
            this(shortText, icon, null);
        }

        public Simple(@Nonnull String shortText, @Nullable Icon icon, @Nullable String fullText) {
            this.shortText = shortText;
            this.icon = icon;
            this.fullText = fullText;
        }

        @Override
        public @Nonnull String getShortText() {
            return shortText;
        }

        @Override
        public @Nullable Icon getIcon() {
            return icon;
        }

        @Override
        public @Nullable String getFullText() {
            return fullText;
        }
    }

    final class ToString implements PopupItemPresentation {
        private final @Nonnull String shortText;

        public ToString(@Nonnull Object value) {
            this.shortText = value.toString();
        }

        @Override
        public @Nonnull String getShortText() {
            return shortText;
        }

        @Override
        public @Nullable Icon getIcon() {
            return null;
        }

        @Override
        public @Nullable String getFullText() {
            return null;
        }
    }
}
