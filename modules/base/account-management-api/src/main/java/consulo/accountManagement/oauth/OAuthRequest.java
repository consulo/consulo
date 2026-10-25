// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.accountManagement.oauth;

import consulo.util.io.Url;

public interface OAuthRequest<T extends OAuthCredentials> {
    /**
     * Url that is used by OAuth flow to redirection when user accepted the OAuth request
     */
    Url getAuthorizationCodeUrl();

    /**
     * Used to exchange code for credentials
     */
    OAuthCredentialsAcquirer<T> getCredentialsAcquirer();

    /**
     * Url that is usually opened in browser where user can accept authorization
     * if PKCE is supported generated hash of code verifier
     */
    Url getAuthUrlWithParameters();
}
