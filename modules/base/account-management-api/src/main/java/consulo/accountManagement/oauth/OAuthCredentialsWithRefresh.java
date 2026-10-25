// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.accountManagement.oauth;

/**
 * Credentials for services where the access token has a limited expiration time
 */
public interface OAuthCredentialsWithRefresh extends OAuthCredentials {
    /**
     * @return token for updating the access token
     */
    String getRefreshToken();

    /**
     * @return access token expiration time
     */
    long getExpiresIn();

    /**
     * Checking the expiration of the token
     */
    boolean isAccessTokenValid();
}
