// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

import java.util.UUID;

/**
 * Base class to represent an account for some external system
 * Properties are abstract to allow marking them with persistence annotations
 * <p>
 * Generally supposed to be used as means of distinguishing multiple credentials from PSafe
 */
public abstract class Account {
    /**
     * An internal unique identifier of an account
     */
    @Nonnull
    public abstract String getId();

    /**
     * Short display name for an account to be shown to a user (login/username/email)
     */
    @Nls
    @Nonnull
    public abstract String getName();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Account otherAccount)) {
            return false;
        }

        return getId().equals(otherAccount.getId());
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    @Nonnull
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
