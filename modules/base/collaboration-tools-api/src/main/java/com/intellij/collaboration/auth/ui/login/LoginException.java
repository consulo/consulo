// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import jakarta.annotation.Nonnull;

public abstract sealed class LoginException extends Exception
    permits LoginException.UnsupportedServerVersion,
    LoginException.InvalidTokenOrUnsupportedServerVersion,
    LoginException.AccountAlreadyExists,
    LoginException.AccountUsernameMismatch {

    public static final class UnsupportedServerVersion extends LoginException {
        @Nonnull
        private final String earliestSupportedVersion;

        public UnsupportedServerVersion(@Nonnull String earliestSupportedVersion) {
            this.earliestSupportedVersion = earliestSupportedVersion;
        }

        @Nonnull
        public String getEarliestSupportedVersion() {
            return earliestSupportedVersion;
        }
    }

    public static final class InvalidTokenOrUnsupportedServerVersion extends LoginException {
        @Nonnull
        private final String earliestSupportedVersion;

        public InvalidTokenOrUnsupportedServerVersion(@Nonnull String earliestSupportedVersion) {
            this.earliestSupportedVersion = earliestSupportedVersion;
        }

        @Nonnull
        public String getEarliestSupportedVersion() {
            return earliestSupportedVersion;
        }
    }

    public static final class AccountAlreadyExists extends LoginException {
        @Nonnull
        private final String username;

        public AccountAlreadyExists(@Nonnull String username) {
            this.username = username;
        }

        @Nonnull
        public String getUsername() {
            return username;
        }
    }

    public static final class AccountUsernameMismatch extends LoginException {
        @Nonnull
        private final String requiredUsername;
        @Nonnull
        private final String username;

        public AccountUsernameMismatch(@Nonnull String requiredUsername, @Nonnull String username) {
            this.requiredUsername = requiredUsername;
            this.username = username;
        }

        @Nonnull
        public String getRequiredUsername() {
            return requiredUsername;
        }

        @Nonnull
        public String getUsername() {
            return username;
        }
    }
}
