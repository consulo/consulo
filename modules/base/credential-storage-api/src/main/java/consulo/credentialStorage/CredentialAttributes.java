/*
 * Copyright 2013-2024 consulo.io
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Consider using #generateServiceName to generate #serviceName.
 */
public record CredentialAttributes(@Nonnull String serviceName, @Nullable String userName, boolean isPasswordMemoryOnly,
                                   boolean cacheDeniedItems) {
    private static final String SERVICE_NAME_PREFIX = "Consulo";

    public CredentialAttributes(@Nonnull String serviceName, @Nullable String userName) {
        this(serviceName, userName, false, true);
    }

    /**
     * The combined name of your service and name of service that requires authentication.
     * <p>
     * Can be specified in:
     * * a reverse-DNS format: `com.apple.facetime: registrationV1`
     * * a prefixed human-readable format: `IntelliJ Platform Settings Repository — github.com`,
     * where `Consulo` prefix **is mandatory**.
     */
    public static String generateServiceName(String subsystem, String key) {
        return SERVICE_NAME_PREFIX + " " + subsystem + " — " + key;
    }
}
