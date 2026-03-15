// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.credentialStorage.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(ComponentScope.APPLICATION)
public interface PasswordSafeSettingsListener {
    Class<PasswordSafeSettingsListener> TOPIC = PasswordSafeSettingsListener.class;

    default void typeChanged(ProviderType oldValue, ProviderType newValue) {
    }

    default void credentialStoreCleared() {
    }
}
