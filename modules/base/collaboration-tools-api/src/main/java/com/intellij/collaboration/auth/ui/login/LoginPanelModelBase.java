// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CancellationException;

public abstract class LoginPanelModelBase implements TokenLoginPanelModel {
    @Nonnull
    private String serverUri = "";
    @Nonnull
    private String token = "";

    private final MutableStateFlow<LoginModel.LoginState> _loginState =
        StateFlowKt.MutableStateFlow(LoginModel.LoginState.Disconnected.INSTANCE);

    @Nonnull
    @Override
    public final StateFlow<LoginModel.LoginState> getLoginState() {
        return FlowKt.asStateFlow(_loginState);
    }

    @Nonnull
    @Override
    public final String getServerUri() {
        return serverUri;
    }

    @Override
    public final void setServerUri(@Nonnull String serverUri) {
        this.serverUri = serverUri;
    }

    @Nonnull
    @Override
    public final String getToken() {
        return token;
    }

    @Override
    public final void setToken(@Nonnull String token) {
        this.token = token;
    }

    @Nullable
    @Override
    public final Object login(@Nonnull Continuation<? super kotlin.Unit> continuation) {
        _loginState.setValue(LoginModel.LoginState.Connecting.INSTANCE);
        try {
            Object result = checkToken(continuation);
            if (result instanceof String username) {
                _loginState.setValue(new LoginModel.LoginState.Connected(username));
            }
            return kotlin.Unit.INSTANCE;
        }
        catch (CancellationException e) {
            _loginState.setValue(LoginModel.LoginState.Disconnected.INSTANCE);
            return kotlin.Unit.INSTANCE;
        }
        catch (Throwable e) {
            _loginState.setValue(new LoginModel.LoginState.Failed(e));
            return kotlin.Unit.INSTANCE;
        }
    }

    @Nullable
    protected abstract Object checkToken(@Nonnull Continuation<? super String> continuation);
}
