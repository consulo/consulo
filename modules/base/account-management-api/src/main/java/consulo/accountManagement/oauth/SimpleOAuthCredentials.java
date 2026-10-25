// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.accountManagement.oauth;

public record SimpleOAuthCredentials(String accessToken) implements OAuthCredentials {
    @Override
    public String getAccessToken() {
        return accessToken;
    }
}
