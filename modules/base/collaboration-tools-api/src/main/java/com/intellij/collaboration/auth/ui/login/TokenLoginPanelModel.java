// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui.login;

import jakarta.annotation.Nonnull;

/**
 * Model for login interface where login is performed via a token
 */
public interface TokenLoginPanelModel extends LoginModel {
    /**
     * URI of a server
     */
    @Nonnull
    String getServerUri();

    void setServerUri(@Nonnull String serverUri);

    /**
     * Access token
     */
    @Nonnull
    String getToken();

    void setToken(@Nonnull String token);
}
