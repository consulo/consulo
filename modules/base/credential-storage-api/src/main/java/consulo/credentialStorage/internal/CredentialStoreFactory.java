// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.credentialStorage.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.credentialStorage.CredentialStore;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface CredentialStoreFactory {
    ExtensionPointName<CredentialStoreFactory> CREDENTIAL_STORE_FACTORY = ExtensionPointName.create(CredentialStoreFactory.class);

    @Nullable
    CredentialStore create();
}
