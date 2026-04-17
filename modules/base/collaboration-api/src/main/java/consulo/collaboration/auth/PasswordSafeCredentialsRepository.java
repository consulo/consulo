// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2025 consulo.io
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
package consulo.collaboration.auth;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import org.jspecify.annotations.Nullable;

/**
 * Generic {@link CredentialsRepository} that stores account credentials through the
 * IDE-wide password safe. This safe is subject to platform changes and is
 * configurable by the user.
 */
public class PasswordSafeCredentialsRepository<A extends Account, Cred>
    implements CredentialsRepository<A, Cred> {

    private final String myServiceName;
    private final CredentialsMapper<Cred> myMapper;

    public PasswordSafeCredentialsRepository(String serviceName, CredentialsMapper<Cred> mapper) {
        myServiceName = serviceName;
        myMapper = mapper;
    }

    @Override
    public void persistCredentials(A account, @Nullable Cred credentials) {
        PasswordSafe passwordSafe = PasswordSafe.getInstance();
        passwordSafe.set(
            credentialAttributes(account),
            credentials != null ? new Credentials(account.getId(), myMapper.serialize(credentials)) : null
        );
    }

    @Override
    public @Nullable Cred retrieveCredentials(A account) {
        PasswordSafe passwordSafe = PasswordSafe.getInstance();
        Credentials credentials = passwordSafe.get(credentialAttributes(account));
        if (credentials == null) {
            return null;
        }
        String password = credentials.getPasswordAsString();
        if (password == null || password.isEmpty()) {
            return null;
        }
        return myMapper.deserialize(password);
    }

    private CredentialAttributes credentialAttributes(A account) {
        return new CredentialAttributes(myServiceName + " — " + account.getId(), account.getId());
    }

    /**
     * Maps credentials to/from string for storage.
     */
    public interface CredentialsMapper<Cred> {
        String serialize(Cred credentials);

        Cred deserialize(String credentials);

        /**
         * Simple mapper for String credentials (token-based auth).
         */
        CredentialsMapper<String> SIMPLE = new CredentialsMapper<>() {
            @Override
            public String serialize(String credentials) {
                return credentials;
            }

            @Override
            public String deserialize(String credentials) {
                return credentials;
            }
        };
    }
}
