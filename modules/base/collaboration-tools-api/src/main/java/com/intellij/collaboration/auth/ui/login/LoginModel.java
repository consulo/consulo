// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;

/**
 * Model for login process
 */
public interface LoginModel {
    /**
     * Represents the current state of the log in process
     */
    @Nonnull
    StateFlow<LoginState> getLoginState();

    /**
     * Performs a login and updates {@link #getLoginState()}
     */
    @Nullable
    Object login(@Nonnull Continuation<? super kotlin.Unit> continuation);

    sealed interface LoginState permits LoginState.Disconnected, LoginState.Connecting, LoginState.Failed, LoginState.Connected {

        final class Disconnected implements LoginState {
            public static final Disconnected INSTANCE = new Disconnected();

            private Disconnected() {
            }
        }

        final class Connecting implements LoginState {
            public static final Connecting INSTANCE = new Connecting();

            private Connecting() {
            }
        }

        final class Failed implements LoginState {
            @Nonnull
            private final Throwable error;

            public Failed(@Nonnull Throwable error) {
                this.error = error;
            }

            @Nonnull
            public Throwable getError() {
                return error;
            }
        }

        final class Connected implements LoginState {
            @Nonnull
            private final String username;

            public Connected(@Nonnull String username) {
                this.username = username;
            }

            @Nonnull
            public String getUsername() {
                return username;
            }
        }
    }

    /**
     * Extension-like utility to get an error flow from a LoginModel
     */
    @Nonnull
    static Flow<Throwable> getErrorFlow(@Nonnull LoginModel model) {
        return FlowKt.transformLatest(model.getLoginState(), (loginState, collector, cont) -> {
            if (loginState instanceof LoginState.Connecting) {
                return collector.emit(null, cont);
            }
            else if (loginState instanceof LoginState.Failed failed) {
                return collector.emit(failed.getError(), cont);
            }
            return kotlin.Unit.INSTANCE;
        });
    }
}
