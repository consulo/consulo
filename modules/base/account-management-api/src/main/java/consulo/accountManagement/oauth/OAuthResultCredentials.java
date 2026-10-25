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

public final class OAuthResultCredentials<T extends OAuthCredentials> {
    public static <T extends OAuthCredentials> OAuthResultCredentials<T> success(@Nullable OAuthRequest<T> request, T credentials) {
        return new OAuthResultCredentials<>(request, credentials, null);
    }

    public static <T extends OAuthCredentials> OAuthResultCredentials<T> failure(@Nullable OAuthRequest<T> request, Throwable error) {
        return new OAuthResultCredentials<>(request, null, error);
    }

    private final @Nullable OAuthRequest<T> myRequest;
    private final @Nullable T myCredentials;
    private final @Nullable Throwable myError;

    private OAuthResultCredentials(@Nullable OAuthRequest<T> request, @Nullable T credentials, @Nullable Throwable error) {
        myRequest = request;
        myCredentials = credentials;
        myError = error;
    }

    public @Nullable OAuthRequest<T> getRequest() {
        return myRequest;
    }

    public @Nullable T getCredentials() {
        return myCredentials;
    }

    public @Nullable Throwable getError() {
        return myError;
    }

    public boolean isAccepted() {
        return myError == null;
    }
}
