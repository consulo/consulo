// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

import java.util.Set;

public interface AvailableClientSettings {
    default boolean getBoilerplateAvailable() {
        return false;
    }

    default Set<String> getFrameworkLanguages() {
        return Set.of();
    }

    default Set<String> getFrameworkVersions() {
        return Set.of();
    }

    default ClientGeneratorSetting getActualClientSettings() {
        return new ClientGeneratorSetting();
    }
}
