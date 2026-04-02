// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.AccountDetails;
import com.intellij.collaboration.auth.AccountManager;
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider;
import com.intellij.collaboration.ui.icon.CachingIconsProvider;
import com.intellij.collaboration.ui.ListModelExtKt;
import consulo.ui.ex.awt.ImageUtil;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Deferred;
import kotlinx.coroutines.DeferredKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LazyLoadingAccountsDetailsProvider<A extends Account, D extends AccountDetails>
    implements LoadingAccountsDetailsProvider<A, D> {

    private final CoroutineScope scope;
    private final Icon defaultAvatarIcon;

    private final MutableStateFlow<Boolean> loadingState;
    private volatile int loadingCount = 0;

    private final MutableSharedFlow<A> loadingCompletionFlow;

    private final ConcurrentHashMap<A, Deferred<Result<D>>> requestsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<A, Result<D>> resultsMap = new ConcurrentHashMap<>();

    private final CachingIconsProvider<A> delegateIconProvider;

    protected LazyLoadingAccountsDetailsProvider(@Nonnull CoroutineScope scope, @Nonnull Icon defaultAvatarIcon) {
        this.scope = scope;
        this.defaultAvatarIcon = defaultAvatarIcon;
        this.loadingState = StateFlowKt.MutableStateFlow(false);
        this.loadingCompletionFlow = SharedFlowKt.MutableSharedFlow(0, 64, kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST);
        this.delegateIconProvider = new CachingIconsProvider<>(createIconProvider());
    }

    @Nonnull
    @Override
    public final MutableStateFlow<Boolean> getLoadingState() {
        return loadingState;
    }

    @Nonnull
    @Override
    public final MutableSharedFlow<A> getLoadingCompletionFlow() {
        return loadingCompletionFlow;
    }

    @Nonnull
    private Deferred<Result<D>> requestDetails(@Nonnull A account) {
        return requestsMap.computeIfAbsent(account, a -> {
            return kotlinx.coroutines.BuildersKt.async(scope, null, null, (s, cont) -> {
                loadingCount++;
                loadingState.setValue(loadingCount > 0);
                try {
                    Result<D> result = loadDetails(a);
                    resultsMap.put(a, result);
                    loadingCompletionFlow.tryEmit(a);
                    return result;
                }
                finally {
                    loadingCount--;
                    loadingState.setValue(loadingCount > 0);
                }
            });
        });
    }

    public void clearDetails(@Nonnull A account) {
        Deferred<Result<D>> deferred = requestsMap.remove(account);
        if (deferred != null) {
            deferred.cancel(null);
        }
        resultsMap.remove(account);
        delegateIconProvider.invalidateAll();
        loadingCompletionFlow.tryEmit(account);
    }

    public void clearOutdatedDetails(@Nonnull Set<A> currentList) {
        for (A account : requestsMap.keySet()) {
            if (!currentList.contains(account)) {
                clearDetails(account);
            }
        }
    }

    @Nonnull
    protected abstract Result<D> loadDetails(@Nonnull A account);

    @Override
    @Nullable
    public final D getDetails(@Nonnull A account) {
        requestDetails(account);
        Result<D> result = resultsMap.get(account);
        if (result instanceof Result.Success<D> success) {
            return success.getDetails();
        }
        return null;
    }

    @Override
    @Nullable
    @Nls
    public final String getErrorText(@Nonnull A account) {
        requestDetails(account);
        Result<D> result = resultsMap.get(account);
        if (result instanceof Result.Error<?> error) {
            return error.getError();
        }
        return null;
    }

    @Override
    public final boolean checkErrorRequiresReLogin(@Nonnull A account) {
        requestDetails(account);
        Result<D> result = resultsMap.get(account);
        return result instanceof Result.Error<?> error && error.isNeedReLogin();
    }

    @Nonnull
    @Override
    public final Icon getIcon(@Nullable A key, int iconSize) {
        return delegateIconProvider.getIcon(key, iconSize);
    }

    @Nonnull
    protected abstract Image loadAvatar(@Nonnull A account, @Nonnull String url);

    public sealed interface Result<D extends AccountDetails> permits Result.Success, Result.Error {
        final class Success<D extends AccountDetails> implements Result<D> {
            private final D details;

            public Success(@Nonnull D details) {
                this.details = details;
            }

            @Nonnull
            public D getDetails() {
                return details;
            }
        }

        final class Error<D extends AccountDetails> implements Result<D> {
            @Nullable
            private final @Nls String error;
            private final boolean needReLogin;

            public Error(@Nullable @Nls String error, boolean needReLogin) {
                this.error = error;
                this.needReLogin = needReLogin;
            }

            @Nullable
            @Nls
            public String getError() {
                return error;
            }

            public boolean isNeedReLogin() {
                return needReLogin;
            }
        }
    }

    @Nonnull
    private AsyncImageIconsProvider<A> createIconProvider() {
        return new AsyncImageIconsProvider<>(scope, new AsyncImageIconsProvider.AsyncImageLoader<>() {
            @Override
            @Nullable
            public Image load(@Nonnull A key) {
                try {
                    Result<D> result = requestDetails(key).await();
                    if (result instanceof Result.Success<D> success) {
                        String url = success.getDetails().getAvatarUrl();
                        if (url == null) {
                            return null;
                        }
                        return loadAvatar(key, url);
                    }
                }
                catch (Exception ignored) {
                }
                return null;
            }

            @Nonnull
            @Override
            public Icon createBaseIcon(@Nullable A key, int iconSize) {
                return IconUtil.resizeSquared(defaultAvatarIcon, iconSize);
            }

            @Nonnull
            @Override
            public Image postProcess(@Nonnull Image image) {
                return ImageUtil.createCircleImage(ImageUtil.toBufferedImage(image));
            }
        });
    }

    /**
     * Add a ListDataListener that monitors changes in the ListModel and clears data for accounts that have been deleted or modified
     * (e.g. if the API token has been updated)
     */
    public static <A extends Account> void cancelOnRemoval(
        @Nonnull LazyLoadingAccountsDetailsProvider<A, ?> provider,
        @Nonnull ListModel<A> listModel
    ) {
        listModel.addListDataListener(new ListDataListener() {
            @Override
            public void contentsChanged(@Nonnull ListDataEvent e) {
                if (e.getIndex0() < 0 || e.getIndex1() < 0) {
                    return;
                }
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    A account = listModel.getElementAt(i);
                    provider.clearDetails(account);
                }
            }

            @Override
            public void intervalRemoved(@Nonnull ListDataEvent e) {
                Set<A> accounts = ListModelExtKt.items(listModel);
                provider.clearOutdatedDetails(accounts);
            }

            @Override
            public void intervalAdded(@Nonnull ListDataEvent e) {
            }
        });
    }

    /**
     * Clears account details when an account is removed or if account credentials are changed
     */
    public static <A extends Account> void cancelOnRemoval(
        @Nonnull LazyLoadingAccountsDetailsProvider<A, ?> provider,
        @Nonnull CoroutineScope scope,
        @Nonnull AccountManager<A, ?> accountManager
    ) {
        kotlinx.coroutines.BuildersKt.launch(
            scope,
            null,
            null,
            (s, cont) -> kotlinx.coroutines.flow.FlowKt.collectLatest(
                accountManager.getAccountsState(),
                accounts -> {
                    provider.clearOutdatedDetails((Set<A>) accounts);
                    return kotlinx.coroutines.CoroutineScopeKt.coroutineScope(
                        (innerScope, innerCont) -> {
                            for (A account : (Set<A>) accounts) {
                                kotlinx.coroutines.BuildersKt.launch(
                                    innerScope,
                                    null,
                                    null,
                                    (s2, cont2) -> accountManager.getCredentialsFlow(account).collect(
                                        creds -> {
                                            provider.clearDetails(account);
                                            return kotlin.Unit.INSTANCE;
                                        },
                                        cont2
                                    )
                                );
                            }
                            return kotlin.Unit.INSTANCE;
                        },
                        cont
                    );
                },
                cont
            )
        );
    }
}
