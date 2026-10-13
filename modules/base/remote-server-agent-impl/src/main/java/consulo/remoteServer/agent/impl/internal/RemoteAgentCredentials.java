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
package consulo.remoteServer.agent.impl.internal;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialAttributesUtil;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

class RemoteAgentCredentials {
    private final PasswordSafe myPasswordSafe;
    private final Executor myExecutor;

    RemoteAgentCredentials(PasswordSafe passwordSafe, Executor executor) {
        myPasswordSafe = passwordSafe;
        myExecutor = executor;
    }

    CompletableFuture<@Nullable String> getTokenAsync(RemoteAgentServerConfiguration configuration) {
        return myPasswordSafe.getAsync(attributes(configuration))
            .thenApply(credentials -> credentials == null ? null : credentials.getPasswordAsString());
    }

    void setToken(RemoteAgentServerConfiguration configuration, @Nullable String token) {
        Credentials credentials = token == null || token.isEmpty() ? null : new Credentials(configuration.getCredentialId(), token);
        CompletableFuture.runAsync(() -> myPasswordSafe.set(attributes(configuration), credentials), myExecutor);
    }

    private static CredentialAttributes attributes(RemoteAgentServerConfiguration configuration) {
        return new CredentialAttributes(CredentialAttributesUtil.generateServiceName("Remote Agent", configuration.getCredentialId()));
    }
}
