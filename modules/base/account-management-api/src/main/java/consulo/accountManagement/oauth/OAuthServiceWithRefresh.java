// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.accountManagement.oauth;

import java.util.concurrent.CompletableFuture;

/**
 * Authorization service with the ability to update an expired access token
 *
 * @param <T> Service credentials, must implement the OAuthCredentialsWithRefresh interface
 */
public interface OAuthServiceWithRefresh<T extends OAuthCredentialsWithRefresh> extends OAuthService<T> {
    CompletableFuture<T> updateAccessToken(RefreshTokenRequest refreshTokenRequest);
}
