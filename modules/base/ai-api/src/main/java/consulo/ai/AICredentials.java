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
package consulo.ai;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * API key storage for providers. Keys go through the platform password safe rather than a provider's
 * own settings, so they are never written into a plain settings file.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public final class AICredentials {
    private static final String SERVICE_PREFIX = "Consulo AI - ";

    public static CompletableFuture<@Nullable String> getApiKey(String providerId) {
        return PasswordSafe.getInstance().getAsync(attributes(providerId)).thenApply(credentials -> {
            String password = credentials == null ? null : credentials.getPasswordAsString();
            return StringUtil.isEmptyOrSpaces(password) ? null : password;
        });
    }

    /**
     * @param apiKey {@code null} or blank clears the stored key
     */
    public static CompletableFuture<?> setApiKey(String providerId, @Nullable String apiKey) {
        Executor executor = Application.get().getInstance(ApplicationConcurrency.class).executor();
        return CompletableFuture.runAsync(
            () -> PasswordSafe.getInstance().set(
                attributes(providerId),
                StringUtil.isEmptyOrSpaces(apiKey) ? null : new Credentials(null, apiKey)),
            executor
        );
    }

    /**
     * The instance name is folded into the service name, and no user name is stored.
     */
    private static CredentialAttributes attributes(String providerId) {
        return new CredentialAttributes(SERVICE_PREFIX + providerId);
    }

    private AICredentials() {
    }
}
