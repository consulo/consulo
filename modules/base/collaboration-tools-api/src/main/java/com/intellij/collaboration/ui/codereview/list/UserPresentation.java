// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Objects;

public interface UserPresentation {
    @Nonnull
    String getUsername();

    @Nullable
    String getFullName();

    @Nonnull
    Icon getAvatarIcon();

    default @Nonnull String getPresentableName() {
        String fullName = getFullName();
        return fullName != null ? fullName : getUsername();
    }

    final class Simple implements UserPresentation {
        private final @Nonnull String username;
        private final @Nullable String fullName;
        private final @Nonnull Icon avatarIcon;

        public Simple(@Nonnull String username, @Nullable String fullName, @Nonnull Icon avatarIcon) {
            this.username = username;
            this.fullName = fullName;
            this.avatarIcon = avatarIcon;
        }

        @Override
        public @Nonnull String getUsername() {
            return username;
        }

        @Override
        public @Nullable String getFullName() {
            return fullName;
        }

        @Override
        public @Nonnull Icon getAvatarIcon() {
            return avatarIcon;
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
            return Objects.equals(username, simple.username) &&
                Objects.equals(fullName, simple.fullName) &&
                Objects.equals(avatarIcon, simple.avatarIcon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, fullName, avatarIcon);
        }

        @Override
        public String toString() {
            return "Simple(username=" + username + ", fullName=" + fullName + ", avatarIcon=" + avatarIcon + ")";
        }
    }
}
