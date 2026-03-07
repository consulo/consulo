// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import com.intellij.credentialStore.*;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.channels.ProducerScope;
import kotlinx.coroutines.flow.CallbackFlowKt;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Generic {@link CredentialsRepository} that stores account credentials through the
 * IDE-wide password safe. This safe is subject to platform changes and is
 * configurable by the user. If the user has not configured it, the default is
 * to try to use the system keychain. Whether this credentials repository can
 * persist credentials is thus dependent on the user's settings for password
 * safes.
 */
public final class PasswordSafeCredentialsRepository<A extends Account, Cred> implements CredentialsRepository<A, Cred> {
    private final String serviceName;
    private final CredentialsMapper<Cred> mapper;

    public PasswordSafeCredentialsRepository(@Nonnull String serviceName, @Nonnull CredentialsMapper<Cred> mapper) {
        this.serviceName = serviceName;
        this.mapper = mapper;
    }

    @Nonnull
    private PasswordSafe getPasswordSafe() {
        return PasswordSafe.getInstance();
    }

    // It is assumed all options other than MEMORY_ONLY persist to disk in some way.
    @Nonnull
    @Override
    public Flow<Boolean> getCanPersistCredentials() {
        return CallbackFlowKt.callbackFlow((ProducerScope<Boolean> scope, Continuation<? super Unit> cont) -> {
            scope.trySend(!getPasswordSafe().isMemoryOnly());
            ApplicationManager.getApplication().getMessageBus().connect(scope)
                .subscribe(PasswordSafeSettings.TOPIC, new PasswordSafeSettingsListener() {
                    @Override
                    public void typeChanged(@Nonnull ProviderType oldValue, @Nonnull ProviderType newValue) {
                        scope.trySend(newValue != ProviderType.MEMORY_ONLY);
                    }
                });
            return kotlinx.coroutines.channels.ChannelKt.awaitClose(scope, cont);
        });
    }

    @Nullable
    @Override
    public Object persistCredentials(@Nonnull A account, @Nullable Cred credentials, @Nonnull Continuation<? super Unit> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.getIO(),
            (scope, cont) -> {
                getPasswordSafe().set(credentialAttributes(account), credentials(account, credentials));
                return Unit.INSTANCE;
            },
            continuation
        );
    }

    @Nullable
    @Override
    public Object retrieveCredentials(@Nonnull A account, @Nonnull Continuation<? super Cred> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.getIO(),
            (scope, cont) -> {
                Credentials creds = getPasswordSafe().get(credentialAttributes(account));
                if (creds == null) {
                    return null;
                }
                String password = creds.getPasswordAsString();
                if (password == null) {
                    return null;
                }
                return mapper.deserialize(password);
            },
            continuation
        );
    }

    @Nonnull
    private CredentialAttributes credentialAttributes(@Nonnull A account) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(serviceName, account.getId()));
    }

    @Nullable
    private Credentials credentials(@Nonnull A account, @Nullable Cred credentials) {
        if (credentials == null) {
            return null;
        }
        return new Credentials(account.getId(), mapper.serialize(credentials));
    }

    public interface CredentialsMapper<Cred> {
        @Nonnull
        String serialize(@Nonnull Cred credentials);

        @Nonnull
        Cred deserialize(@Nonnull String credentials);

        CredentialsMapper<String> Simple = new CredentialsMapper<>() {
            @Nonnull
            @Override
            public String serialize(@Nonnull String credentials) {
                return credentials;
            }

            @Nonnull
            @Override
            public String deserialize(@Nonnull String credentials) {
                return credentials;
            }
        };
    }
}
