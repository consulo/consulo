// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import com.intellij.concurrency.ConcurrentCollectionFactory;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AccountUrlAuthenticationFailuresHolder<A extends Account> {
    private final CoroutineScope cs;
    private final AccountManagerProvider<A> accountManagerProvider;
    private final ConcurrentHashMap<A, Set<String>> storeMap = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface AccountManagerProvider<A extends Account> {
        @Nonnull
        AccountManager<A, ?> get();
    }

    public AccountUrlAuthenticationFailuresHolder(
        @Nonnull CoroutineScope cs,
        @Nonnull AccountManagerProvider<A> accountManagerProvider
    ) {
        this.cs = cs;
        this.accountManagerProvider = accountManagerProvider;

        Job job = (Job) cs.getCoroutineContext().get(Job.Key);
        if (job != null) {
            job.invokeOnCompletion(cause -> {
                storeMap.clear();
                return Unit.INSTANCE;
            });
        }
    }

    public void markFailed(@Nonnull A account, @Nonnull String url) {
        storeMap.computeIfAbsent(
            account,
            a -> {
                BuildersKt.launch(
                    cs,
                    CoroutineNameKt.CoroutineName("AccountUrlAuthenticationFailuresHolder token change listener"),
                    CoroutineStart.DEFAULT,
                    (scope, cont) -> FlowKt.first(accountManagerProvider.get().getCredentialsFlow(account), cont)
                );
                // After first credential change, remove from store
                // Note: the launch above will handle the removal asynchronously
                return ConcurrentCollectionFactory.createConcurrentSet();
            }
        ).add(url);
    }

    public boolean isFailed(@Nonnull A account, @Nonnull String url) {
        Set<String> urls = storeMap.get(account);
        return urls != null && urls.contains(url);
    }
}
