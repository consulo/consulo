// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import consulo.logging.Logger;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.NonCancellable;
import kotlinx.coroutines.flow.*;
import kotlinx.coroutines.sync.Mutex;
import kotlinx.coroutines.sync.MutexKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * Base class for account management application service.
 * Accounts are stored in {@link #accountsRepository()}.
 * Credentials are stored in {@link #credentialsRepository()}.
 */
public abstract class AccountManagerBase<A extends Account, Cred> implements AccountManager<A, Cred> {
    private final Logger logger;

    private volatile Flow<Boolean> canPersistCredentialsLazy;
    private final MutableStateFlow<Set<A>> _accountsState;
    private final StateFlow<Set<A>> accountsState;
    private final MutableSharedFlow<Object> accountsEventsFlow;
    private final Mutex mutex;

    protected AccountManagerBase(@Nonnull Logger logger) {
        this.logger = logger;
        this._accountsState = StateFlowKt.MutableStateFlow(accountsRepository().getAccounts());
        AccountsRepository<A> repo = accountsRepository();
        if (repo instanceof ObservableAccountsRepository<A> observable) {
            this.accountsState = observable.getAccountsFlow();
        }
        else {
            this.accountsState = FlowKt.asStateFlow(_accountsState);
        }
        this.accountsEventsFlow = SharedFlowKt.MutableSharedFlow(0, 64, BufferOverflow.SUSPEND);
        this.mutex = MutexKt.Mutex(false);
    }

    @Nonnull
    protected abstract AccountsRepository<A> accountsRepository();

    @Nonnull
    protected abstract CredentialsRepository<A, Cred> credentialsRepository();

    @Nonnull
    @Override
    public Flow<Boolean> getCanPersistCredentials() {
        Flow<Boolean> result = canPersistCredentialsLazy;
        if (result == null) {
            synchronized (this) {
                result = canPersistCredentialsLazy;
                if (result == null) {
                    result = credentialsRepository().getCanPersistCredentials();
                    canPersistCredentialsLazy = result;
                }
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public StateFlow<Set<A>> getAccountsState() {
        return accountsState;
    }

    @Nullable
    @Override
    public Object updateAccounts(
        @Nonnull Map<A, ? extends Cred> accountsWithCredentials,
        @Nonnull Continuation<? super Unit> continuation
    ) {
        return BuildersKt.withContext(Dispatchers.getDefault(), (scope, cont) -> {
            return MutexKt.withLock(mutex, null, (cont2) -> {
                return BuildersKt.withContext(NonCancellable.INSTANCE, (scope2, cont3) -> {
                    Set<A> currentSet = accountsRepository().getAccounts();
                    Set<A> removed = new LinkedHashSet<>(currentSet);
                    removed.removeAll(accountsWithCredentials.keySet());
                    for (A account : removed) {
                        saveCredentialsSafe(account, null, cont3);
                    }

                    for (Map.Entry<A, ? extends Cred> entry : accountsWithCredentials.entrySet()) {
                        if (entry.getValue() != null) {
                            saveCredentialsSafe(entry.getKey(), entry.getValue(), cont3);
                        }
                    }
                    Set<A> added = new LinkedHashSet<>(accountsWithCredentials.keySet());
                    added.removeAll(currentSet);
                    if (!added.isEmpty() || !removed.isEmpty()) {
                        accountsRepository().setAccounts(accountsWithCredentials.keySet());
                        _accountsState.setValue(accountsWithCredentials.keySet());
                        logger.debug("Account list changed to: " + accountsRepository().getAccounts());
                    }
                    accountsEventsFlow.tryEmit(new AccountsRemoved<>(removed));
                    accountsEventsFlow.tryEmit(new AccountsAddedOrUpdated<>(accountsWithCredentials));
                    return Unit.INSTANCE;
                }, cont2);
            }, cont);
        }, continuation);
    }

    @Nullable
    @Override
    public Object updateAccount(@Nonnull A account, @Nonnull Cred credentials, @Nonnull Continuation<? super Unit> continuation) {
        return BuildersKt.withContext(
            Dispatchers.getDefault(),
            (scope, cont) -> MutexKt.withLock(
                mutex,
                null,
                (cont2) -> BuildersKt.withContext(
                    NonCancellable.INSTANCE,
                    (scope2, cont3) -> {
                        Set<A> currentSet = accountsRepository().getAccounts();
                        boolean isNewAccount = !currentSet.contains(account);
                        Set<A> newSet;
                        if (!isNewAccount) {
                            newSet = new LinkedHashSet<>(currentSet);
                            newSet.remove(account);
                            newSet.add(account);
                        }
                        else {
                            logger.debug("Added new account: " + account);
                            newSet = new LinkedHashSet<>(currentSet);
                            newSet.add(account);
                        }
                        saveCredentialsSafe(account, credentials, cont3);
                        accountsRepository().setAccounts(newSet);
                        _accountsState.setValue(newSet);
                        accountsEventsFlow.tryEmit(new AccountsAddedOrUpdated<>(Map.of(account, credentials)));
                        logger.debug("Updated credentials for account: " + account);
                        return Unit.INSTANCE;
                    },
                    cont2
                ),
                cont
            ),
            continuation
        );
    }

    @Nullable
    @Override
    public Object removeAccount(@Nonnull A account, @Nonnull Continuation<? super Unit> continuation) {
        return BuildersKt.withContext(
            Dispatchers.getDefault(),
            (scope, cont) -> MutexKt.withLock(
                mutex,
                null,
                (cont2) -> BuildersKt.withContext(
                    NonCancellable.INSTANCE,
                    (scope2, cont3) -> {
                        Set<A> currentSet = accountsRepository().getAccounts();
                        Set<A> newSet = new LinkedHashSet<>(currentSet);
                        newSet.remove(account);
                        if (newSet.size() != currentSet.size()) {
                            saveCredentialsSafe(account, null, cont3);
                            accountsRepository().setAccounts(newSet);
                            _accountsState.setValue(newSet);
                            accountsEventsFlow.tryEmit(new AccountsRemoved<>(Set.of(account)));
                            logger.debug("Removed account: " + account);
                        }
                        return Unit.INSTANCE;
                    },
                    cont2
                ),
                cont
            ),
            continuation
        );
    }

    private Object saveCredentialsSafe(@Nonnull A account, @Nullable Cred credentials, @Nonnull Continuation<? super Unit> continuation) {
        return BuildersKt.withContext(
            Dispatchers.getIO(),
            (scope, cont) -> {
                try {
                    return credentialsRepository().persistCredentials(account, credentials, cont);
                }
                catch (Exception e) {
                    logger.warn(e);
                    return Unit.INSTANCE;
                }
            },
            continuation
        );
    }

    @Nullable
    @Override
    public Object findCredentials(@Nonnull A account, @Nonnull Continuation<? super Cred> continuation) {
        return credentialsRepository().retrieveCredentials(account, continuation);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Object getCredentialsState(
        @Nonnull CoroutineScope scope,
        @Nonnull A account,
        @Nonnull Continuation<? super StateFlow<Cred>> continuation
    ) {
        return BuildersKt.withContext(
            scope.getCoroutineContext().plus(Dispatchers.getDefault()),
            (innerScope, cont) -> MutexKt.withLock(
                mutex,
                null,
                (cont2) -> {
                    Object creds = findCredentials(account, cont2);
                    return FlowKt.stateIn(getCredentialsFlow(account), scope, SharingStarted.Companion.getEagerly(), creds);
                },
                cont
            ),
            continuation
        );
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Flow<Cred> getCredentialsFlow(@Nonnull A account) {
        return FlowKt.flowOn(
            FlowKt.transform(
                accountsEventsFlow,
                (event, collector, cont) -> {
                    if (event instanceof AccountsAddedOrUpdated<?, ?> addedOrUpdated) {
                        Object creds = ((AccountsAddedOrUpdated<A, Cred>) addedOrUpdated).getMap().get(account);
                        if (creds != null) {
                            return collector.emit((Cred) creds, cont);
                        }
                    }
                    else if (event instanceof AccountsRemoved<?, ?> removed) {
                        if (((AccountsRemoved<A, ?>) removed).getAccounts().contains(account)) {
                            return collector.emit(null, cont);
                        }
                    }
                    return Unit.INSTANCE;
                }
            ),
            Dispatchers.getDefault()
        );
    }

    private static final class AccountsRemoved<A, Cred> {
        private final Set<A> accounts;

        AccountsRemoved(@Nonnull Set<A> accounts) {
            this.accounts = accounts;
        }

        @Nonnull
        Set<A> getAccounts() {
            return accounts;
        }
    }

    private static final class AccountsAddedOrUpdated<A, Cred> {
        private final Map<A, ? extends Cred> map;

        AccountsAddedOrUpdated(@Nonnull Map<A, ? extends Cred> map) {
            this.map = map;
        }

        @Nonnull
        Map<A, ? extends Cred> getMap() {
            return map;
        }
    }
}
