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

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * API key storage for providers. Keys go through the platform password safe rather than a provider's
 * own settings, so they are never written into a plain settings file.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public final class AICredentials {
    private static final String SERVICE_PREFIX = "Consulo AI - ";

    public static @Nullable String getApiKey(String providerId) {
        Credentials credentials = PasswordSafe.getInstance().get(attributes(providerId));
        String password = credentials == null ? null : credentials.getPasswordAsString();
        return StringUtil.isEmptyOrSpaces(password) ? null : password;
    }

    public static boolean hasApiKey(String providerId) {
        return getApiKey(providerId) != null;
    }

    /**
     * @param apiKey {@code null} or blank clears the stored key
     */
    public static void setApiKey(String providerId, @Nullable String apiKey) {
        PasswordSafe.getInstance().set(
            attributes(providerId),
            StringUtil.isEmptyOrSpaces(apiKey) ? null : new Credentials(null, apiKey));
    }

    /**
     * The instance name is folded into the service name and no user name is used anywhere - neither
     * here nor on the stored {@link Credentials}. The KeePass-backed store keys entries by
     * (service, user) but derives the user differently on write, read and delete, so any non-null
     * user name makes the three disagree: a stored key then either cannot be read back or survives
     * deletion and leaks to the next instance that reuses the name.
     */
    private static CredentialAttributes attributes(String providerId) {
        return new CredentialAttributes(SERVICE_PREFIX + providerId);
    }

    private AICredentials() {
    }
}
