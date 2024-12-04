/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.credentialStorage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.credentialStorage.ui.PasswordSafePromptDialog;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @see PasswordSafePromptDialog
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PasswordSafe {

    /**
     * @return the instance of password safe service
     */
    public static PasswordSafe getInstance() {
        return Application.get().getInstance(PasswordSafe.class);
    }

    /**
     * Get password stored in a password safe
     *
     * @param project   the project, that is used to ask for the master password if this is the first access to password safe
     * @param requester the requester class
     * @param key       the key for the password
     * @return the stored password or null if the password record was not found or was removed
     * @throws PasswordSafeException if password safe cannot be accessed
     */
    @Nullable
    String getPassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException;

    /**
     * Remove password stored in a password safe
     *
     * @param project   the project, that is used to ask for the master password if this is the first access to password safe
     * @param requester the requester class
     * @param key       the key for the password
     * @return the plugin key
     * @throws PasswordSafeException if password safe cannot be accessed
     */
    void removePassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException;

    /**
     * Store password in password safe
     *
     * @param project   the project, that is used to ask for the master password if this is the first access to password safe
     * @param requester the requester class
     * @param key       the key for the password
     * @param value     the value to store
     * @throws PasswordSafeException if password safe cannot be accessed
     */
    default void storePassword(@Nullable Project project, Class requester, String key, String value) throws PasswordSafeException {
        storePassword(project, requester, key, value, true);
    }

    /**
     * Store password in password safe
     *
     * @param project        the project, that is used to ask for the master password if this is the first access to password safe
     * @param requester      the requester class
     * @param key            the key for the password
     * @param value          the value to store
     * @param recordPassword - record password if current provider allow it(memory - will not remember after restart)
     * @throws PasswordSafeException if password safe cannot be accessed
     */
    void storePassword(@Nullable Project project,
                       Class requester,
                       String key,
                       String value,
                       boolean recordPassword) throws PasswordSafeException;

    @Nullable
    default Credentials get(@Nonnull CredentialAttributes attributes) {
        throw new UnsupportedOperationException("not implemented"); // TODO not implemented
    }

    default void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials, boolean memoryOnly) {
        throw new UnsupportedOperationException("not implemented"); // TODO not implemented
    }
}
