// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.accountManagement.oauth;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * General interface for authorization services
 *
 * @param <T> Service credentials, must implement the OAuthCredentials interface
 */
public interface OAuthService<T extends OAuthCredentials> {
    /**
     * @return name of the service
     */
    String getName();

    /**
     * Starting the authorization flow
     */
    CompletableFuture<T> authorize(OAuthRequest<T> request);

    /**
     * Revoking the access token
     */
    void revokeToken(String token);

    @Nullable
    OAuthResult<T> handleOAuthServerCallback(String path, Map<String, List<String>> parameters);

    @Nullable
    OAuthResultCredentials<T> handleOAuthServerCredentialsCallback(String path, Map<String, List<String>> parameters);
}
