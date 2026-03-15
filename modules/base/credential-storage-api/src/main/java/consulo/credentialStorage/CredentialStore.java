// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.credentialStorage;

import org.jspecify.annotations.Nullable;

/**
 * Please see <a href="https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html">Storing Sensitive Data</a>.
 */
public interface CredentialStore {
    @Nullable
    Credentials get(CredentialAttributes attributes);

    default @Nullable String getPassword(CredentialAttributes attributes) {
        var credentials = get(attributes);
        return credentials == null ? null : credentials.getPasswordAsString();
    }

    void set(CredentialAttributes attributes, @Nullable Credentials credentials);

    default void setPassword(CredentialAttributes attributes, @Nullable String password) {
        set(attributes, password == null ? null : new Credentials(attributes.getUserName(), password));
    }
}
