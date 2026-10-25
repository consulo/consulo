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

import consulo.accountManagement.oauth.AcceptCodeHandleResult;
import consulo.accountManagement.oauth.OAuthCallbackHandler;
import consulo.accountManagement.oauth.OAuthResult;
import consulo.accountManagement.oauth.OAuthService;
import consulo.accountManagement.oauth.RedirectAcceptCodeHandleResult;
import consulo.annotation.component.ExtensionImpl;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandHubOAuthCallbackHandler extends OAuthCallbackHandler {
    private final SandHubOAuthService myService;

    @Inject
    public SandHubOAuthCallbackHandler(SandHubOAuthService service) {
        myService = service;
    }

    @Override
    protected OAuthService<?> oauthService() {
        return myService;
    }

    @Override
    protected AcceptCodeHandleResult handleOAuthResult(OAuthResult<?> oAuthResult) {
        return new RedirectAcceptCodeHandleResult(myService.serviceUrl().resolve(oAuthResult.isAccepted() ? "complete" : "error"));
    }
}
