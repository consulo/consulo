/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.sandboxPlugin.ide.account;

import consulo.accountManagement.oauth.OAuthCredentials;
import consulo.accountManagement.oauth.OAuthCredentialsAcquirer;
import consulo.accountManagement.oauth.OAuthCredentialsAcquirerHttp;
import consulo.accountManagement.oauth.OAuthRequest;
import consulo.accountManagement.oauth.OAuthServiceBase;
import consulo.accountManagement.oauth.PkceUtils;
import consulo.accountManagement.oauth.SimpleOAuthCredentials;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import jakarta.inject.Singleton;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class SandHubOAuthService extends OAuthServiceBase<OAuthCredentials> {
    public static final String SERVICE_NAME = "sandHub/oauth";

    Url serviceUrl() {
        return Objects.requireNonNull(Urls.newFromEncoded(SandHubFakeOAuthServer.URL));
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    public CompletableFuture<OAuthCredentials> authorize() {
        return authorize(new SandHubOAuthRequest(serviceUrl()));
    }

    @Override
    public void revokeToken(String token) {
    }

    private static final class SandHubOAuthRequest implements OAuthRequest<OAuthCredentials> {
        private final Url myServiceUrl;
        private final String myCodeVerifier = PkceUtils.generateCodeVerifier();
        private final String myCodeChallenge = PkceUtils.generateShaCodeChallenge(myCodeVerifier, Base64.getUrlEncoder());

        private SandHubOAuthRequest(Url serviceUrl) {
            myServiceUrl = serviceUrl;
        }

        @Override
        public Url getAuthorizationCodeUrl() {
            return Objects.requireNonNull(Urls.newFromEncoded(
                "http://127.0.0.1:" + BuiltInServerManager.getInstance().getPort() + "/api/" + SERVICE_NAME + "/authorization_code"
            ));
        }

        @Override
        public OAuthCredentialsAcquirer<OAuthCredentials> getCredentialsAcquirer() {
            return code -> OAuthCredentialsAcquirerHttp.requestToken(
                myServiceUrl.resolve("access_token").addParameters(Map.of("code", code, "code_verifier", myCodeVerifier)),
                (body, headers) -> new SimpleOAuthCredentials(headers.firstValue("X-OAuth-Token").orElseThrow())
            );
        }

        @Override
        public Url getAuthUrlWithParameters() {
            return myServiceUrl.resolve("authorize").addParameters(Map.of(
                "code_challenge", myCodeChallenge,
                "callback_url", getAuthorizationCodeUrl().toExternalForm()
            ));
        }
    }
}
