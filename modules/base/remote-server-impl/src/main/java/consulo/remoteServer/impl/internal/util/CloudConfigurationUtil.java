// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.util;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CloudConfigurationUtil {

    public static void doSetSafeValue(@Nullable CredentialAttributes credentialAttributes,
                                      @Nullable String credentialUser,
                                      @Nullable String secretValue) {

        doSetSafeValue(credentialAttributes, credentialUser, secretValue, value -> {
        });
    }

    public static void doSetSafeValue(@Nullable CredentialAttributes credentialAttributes,
                                      @Nullable String credentialUser,
                                      @Nullable String secretValue,
                                      @Nonnull Consumer<? super String> unsafeSetter) {

        if (credentialAttributes != null) {
            PasswordSafe.getInstance().set(credentialAttributes, new Credentials(credentialUser, secretValue), false);
            unsafeSetter.accept(null);
        }
        else {
            unsafeSetter.accept(secretValue);
        }
    }

    public static Optional<String> doGetSafeValue(@Nullable CredentialAttributes credentialAttributes) {
        return Optional.ofNullable(credentialAttributes)
            .map(attributes -> PasswordSafe.getInstance().get(attributes))
            .map(Credentials::getPasswordAsString);
    }

    public static String doGetSafeValue(@Nullable CredentialAttributes credentialAttributes, @Nonnull Supplier<String> unsafeGetter) {
        return doGetSafeValue(credentialAttributes).orElseGet(unsafeGetter);
    }

    public static boolean hasSafeCredentials(@Nullable CredentialAttributes credentialAttributes) {
        return credentialAttributes != null && PasswordSafe.getInstance().get(credentialAttributes) != null;
    }

    public static @Nullable CredentialAttributes createCredentialAttributes(String serviceName, String credentialsUser) {
        return StringUtil.isEmpty(serviceName) || StringUtil.isEmpty(credentialsUser)
            ? null
            : new CredentialAttributes(serviceName, credentialsUser);
    }
}
