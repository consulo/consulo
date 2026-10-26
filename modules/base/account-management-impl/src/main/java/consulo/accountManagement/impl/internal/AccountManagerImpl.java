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
package consulo.accountManagement.impl.internal;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountCredentialSource;
import consulo.accountManagement.AccountCredentials;
import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountFeatureState;
import consulo.accountManagement.AccountLevel;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountScope;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.impl.internal.state.AccountApplicationState;
import consulo.accountManagement.impl.internal.state.AccountEntry;
import consulo.accountManagement.impl.internal.state.AccountListState;
import consulo.accountManagement.impl.internal.state.AccountProjectState;
import consulo.accountManagement.impl.internal.ui.AccountLoginDialogDescriptor;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.http.HttpRequests;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@Singleton
@ServiceImpl
public class AccountManagerImpl implements AccountManager {
    private static final Logger LOG = Logger.getInstance(AccountManagerImpl.class);

    private final Application myApplication;
    private final AccountApplicationState myStateHolder;
    private final Object myLock = new Object();
    private final Map<String, AccountImpl> myAccounts = new ConcurrentHashMap<>();
    private final Map<String, MutableObservableValue<@Nullable Image>> myAvatars = new ConcurrentHashMap<>();
    private final List<AccountListener> myListeners = new CopyOnWriteArrayList<>();

    @Inject
    public AccountManagerImpl(Application application, AccountApplicationState state) {
        myApplication = application;
        myStateHolder = state;
    }

    @Override
    public List<AccountType<?>> getAccountTypes() {
        List<AccountType<?>> result = new ArrayList<>();
        myApplication.getExtensionPoint(AccountType.class).forEach(result::add);
        return result;
    }

    @Override
    public @Nullable AccountType<?> findAccountType(String id) {
        return myApplication.getExtensionPoint(AccountType.class).findFirstSafe(type -> type.getId().equals(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends AccountScope> List<AccountFeature<S>> getFeatures(AccountType<S> type) {
        List<AccountFeature<S>> result = new ArrayList<>();
        myApplication.getExtensionPoint(AccountFeature.class).forEach(feature -> {
            if (feature.getAccountType().isInstance(type)) {
                result.add((AccountFeature<S>) feature);
            }
        });
        return result;
    }

    @Override
    public List<? extends AccountAuthorizeConfig> getAuthorizeConfigs(AccountType<?> type) {
        List<AccountAuthorizeConfig> result = new ArrayList<>();
        for (AccountAuthorizeConfig config : type.getAuthorizeConfigs()) {
            if (findAuthorizeType(config) != null) {
                result.add(config);
            }
        }
        return result;
    }

    @Override
    public @Nullable AccountAuthorizeType<?> findAuthorizeType(AccountAuthorizeConfig config) {
        return findAuthorizeType(config.getAuthorizeTypeId());
    }

    @Override
    public @Nullable AccountAuthorizeType<?> findAuthorizeType(String id) {
        return myApplication.getExtensionPoint(AccountAuthorizeType.class).findFirstSafe(type -> type.getId().equals(id));
    }

    public @Nullable AccountCredentialSource findCredentialSource(String id) {
        return myApplication.getExtensionPoint(AccountCredentialSource.class).findFirstSafe(source -> source.getId().equals(id));
    }

    public @Nullable AccountAuthorizeConfig findConfig(AccountType<?> type, String authorizeTypeId) {
        for (AccountAuthorizeConfig config : type.getAuthorizeConfigs()) {
            if (config.getAuthorizeTypeId().equals(authorizeTypeId)) {
                return config;
            }
        }
        return null;
    }

    @Override
    public List<Account> getAccounts(@Nullable Project project) {
        List<Account> result = new ArrayList<>();
        synchronized (myLock) {
            for (AccountEntry entry : appState().accounts()) {
                result.add(wrap(entry, AccountLevel.APPLICATION));
            }
            AccountListState projectState = projectState(project);
            if (projectState != null) {
                for (AccountEntry entry : projectState.accounts()) {
                    result.add(wrap(entry, AccountLevel.PROJECT));
                }
            }
        }
        return result;
    }

    @Override
    public List<Account> getAccounts(AccountType<?> type, @Nullable Project project) {
        List<Account> result = new ArrayList<>();
        for (Account account : getAccounts(project)) {
            if (account.getTypeId().equals(type.getId())) {
                result.add(account);
            }
        }
        return result;
    }

    @Override
    public @Nullable Account findAccount(String id, @Nullable Project project) {
        for (Account account : getAccounts(project)) {
            if (account.getId().equals(id)) {
                return account;
            }
        }
        return null;
    }

    @Override
    public @Nullable Account getActiveAccount(AccountType<?> type, @Nullable Project project) {
        AccountListState projectState = projectState(project);
        if (projectState != null) {
            String projectActive = projectState.active().get(type.getId());
            Account account = projectActive == null ? null : findAccount(projectActive, project);
            if (account != null) {
                return account;
            }
        }
        String active = appState().active().get(type.getId());
        Account account = active == null ? null : findAccount(active, project);
        if (account != null) {
            return account;
        }
        List<Account> accounts = getAccounts(type, project);
        return accounts.isEmpty() ? null : accounts.getFirst();
    }

    @Override
    public void setActiveAccount(AccountType<?> type, @Nullable Account account, @Nullable Project project) {
        synchronized (myLock) {
            AccountListState state = project == null ? appState() : Objects.requireNonNull(projectState(project));
            if (account == null) {
                state.active().remove(type.getId());
            }
            else {
                state.active().put(type.getId(), account.getId());
            }
        }
        for (AccountListener listener : myListeners) {
            listener.activeAccountChanged(type, account);
        }
    }

    @Override
    public void moveAccount(Account account, AccountLevel level, @Nullable Project project) {
        if (account.getLevel() == level) {
            return;
        }
        AccountListState projectState = projectState(project);
        if (projectState == null) {
            return;
        }
        AccountImpl impl = (AccountImpl) account;
        synchronized (myLock) {
            AccountListState from = level == AccountLevel.PROJECT ? appState() : projectState;
            AccountListState to = level == AccountLevel.PROJECT ? projectState : appState();
            from.accounts().remove(impl.getEntry());
            to.accounts().add(impl.getEntry());
            from.active().values().removeIf(id -> id.equals(account.getId()));
            impl.setLevel(level);
        }
        fireAccountsChanged();
    }

    @Override
    public CompletableFuture<?> removeAccount(Account account, @Nullable Project project) {
        AccountImpl impl = (AccountImpl) account;
        synchronized (myLock) {
            appState().accounts().remove(impl.getEntry());
            appState().active().values().removeIf(id -> id.equals(account.getId()));
            AccountListState projectState = projectState(project);
            if (projectState != null) {
                projectState.accounts().remove(impl.getEntry());
                projectState.active().values().removeIf(id -> id.equals(account.getId()));
            }
        }
        myAccounts.remove(account.getId());
        myAvatars.remove(account.getId());
        fireAccountsChanged();

        AccountCredentialSource source = findCredentialSource(account.getCredentialSourceId());
        if (source == null || !source.isWritable()) {
            return CompletableFuture.completedFuture(null);
        }
        return source.store(account, null);
    }

    @Override
    public CompletableFuture<AccountConnection> connect(Account account) {
        AccountType<?> type = findAccountType(account.getTypeId());
        AccountAuthorizeType<?> authorizeType = findAuthorizeType(account.getAuthorizeTypeId());
        AccountCredentialSource source = findCredentialSource(account.getCredentialSourceId());
        AccountAuthorizeConfig config = type == null ? null : findConfig(type, account.getAuthorizeTypeId());
        if (type == null || authorizeType == null || source == null || config == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Account " + account + " is not supported"));
        }
        return source.load(account).thenCompose(value -> {
            if (value == null) {
                markAuthFailed(account);
                return CompletableFuture.failedFuture(new IllegalStateException("No credentials for " + account));
            }
            return connect(account, authorizeType, config, value, source);
        });
    }

    private <C extends AccountCredentials> CompletableFuture<AccountConnection> connect(
        Account account,
        AccountAuthorizeType<C> authorizeType,
        AccountAuthorizeConfig config,
        String value,
        AccountCredentialSource source
    ) {
        C credentials = authorizeType.deserialize(value, config);
        if (!authorizeType.isExpired(credentials)) {
            return CompletableFuture.completedFuture(new AccountConnectionImpl<>(account.getServerUrl(), authorizeType, config, credentials));
        }
        return authorizeType.refresh(credentials, config).thenCompose(refreshed -> {
            AccountConnectionImpl<C> connection = new AccountConnectionImpl<>(account.getServerUrl(), authorizeType, config, refreshed);
            return source.store(account, connection.serialize()).thenApply(ignored -> (AccountConnection) connection);
        }).whenComplete((connection, error) -> {
            if (error != null) {
                markAuthFailed(account);
            }
        });
    }

    @Override
    public <S extends AccountScope> AccountFeatureState getState(AccountFeature<S> feature, Account account) {
        if (account.isAuthFailed()) {
            return AccountFeatureState.AUTH_FAILED;
        }
        AccountType<S> type = typeOf(feature, account);
        if (type == null) {
            return AccountFeatureState.UNKNOWN;
        }
        return type.getState(feature, grantedScopes(type, account));
    }

    @Override
    public <S extends AccountScope> Set<S> getMissingScopes(AccountFeature<S> feature, Account account) {
        AccountType<S> type = typeOf(feature, account);
        if (type == null) {
            return Set.of();
        }
        Set<S> missing = new LinkedHashSet<>(feature.getRequiredScopes());
        missing.removeAll(grantedScopes(type, account));
        return missing;
    }

    @SuppressWarnings("unchecked")
    private <S extends AccountScope> @Nullable AccountType<S> typeOf(AccountFeature<S> feature, Account account) {
        AccountType<?> type = findAccountType(account.getTypeId());
        if (type == null || !feature.getAccountType().isInstance(type)) {
            return null;
        }
        return (AccountType<S>) type;
    }

    public static <S extends AccountScope> Set<S> grantedScopes(AccountType<S> type, Account account) {
        Set<String> ids = account.getGrantedScopeIds();
        Set<S> result = new LinkedHashSet<>();
        for (S scope : type.getAllScopes()) {
            if (ids.contains(scope.getId())) {
                result.add(scope);
            }
        }
        return result;
    }

    @Override
    public void markAuthFailed(Account account) {
        if (account instanceof AccountImpl impl && !impl.isAuthFailed()) {
            impl.setAuthFailed(true);
            fireAccountChanged(account);
        }
    }

    @Override
    public ObservableValue<@Nullable Image> getAvatar(Account account) {
        return myAvatars.computeIfAbsent(account.getId(), id -> {
            MutableObservableValue<@Nullable Image> value = MutableObservableValue.of(null);
            loadAvatar(account, value);
            return value;
        });
    }

    private void loadAvatar(Account account, MutableObservableValue<@Nullable Image> value) {
        String url = account.getAvatarUrl();
        if (url == null) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = HttpRequests.request(url).readBytes(null);
                return Image.fromBytes(Image.ImageType.PNG, bytes);
            }
            catch (Exception e) {
                LOG.warn("Failed to load avatar of " + account + " from " + url, e);
                return null;
            }
        }, myApplication.getInstance(ApplicationConcurrency.class).executor()).thenAccept(value::set);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<@Nullable Account> requestLogin(
        AccountType<?> type,
        @Nullable AccountAuthorizeConfig config,
        @Nullable Project project,
        @Nullable Component parent
    ) {
        AccountAuthorizeConfig chosen = config;
        if (chosen == null) {
            List<? extends AccountAuthorizeConfig> configs = getAuthorizeConfigs(type);
            for (AccountAuthorizeConfig candidate : configs) {
                if (candidate.isDefault()) {
                    chosen = candidate;
                }
            }
            if (chosen == null && !configs.isEmpty()) {
                chosen = configs.getFirst();
            }
        }
        if (chosen == null) {
            return CompletableFuture.completedFuture(null);
        }
        return AccountLoginDialogDescriptor.show(this, type, chosen, null, null, project, parent)
            .thenCompose(result -> result == null ? CompletableFuture.<@Nullable Account>completedFuture(null) : addAccount(type, result, project));
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<@Nullable Account> requestAuthorization(
        Account account,
        @Nullable AccountFeature<?> feature,
        @Nullable Project project,
        @Nullable Component parent
    ) {
        AccountType<?> type = findAccountType(account.getTypeId());
        AccountAuthorizeConfig config = type == null ? null : findConfig(type, account.getAuthorizeTypeId());
        if (type == null || config == null) {
            return CompletableFuture.completedFuture(null);
        }
        return AccountLoginDialogDescriptor.show(this, type, config, account, feature, project, parent)
            .thenCompose(result -> result == null ? CompletableFuture.completedFuture(null) : updateAccount((AccountImpl) account, result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends AccountCredentials> CompletableFuture<Account> addAccount(
        AccountType<?> type,
        AccountAuthorizeConfig config,
        C credentials,
        @Nullable String serverUrl
    ) {
        AccountAuthorizeType<C> authorizeType = (AccountAuthorizeType<C>) findAuthorizeType(config);
        if (authorizeType == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown authorize type " + config.getAuthorizeTypeId()));
        }
        AccountConnectionImpl<C> connection = new AccountConnectionImpl<>(serverUrl, authorizeType, config, credentials);
        Set<String> scopeIds = new LinkedHashSet<>();
        for (AccountScope scope : type.getBaseScopes()) {
            scopeIds.add(scope.getId());
        }
        return completeLogin(type, config, connection, scopeIds).thenCompose(result -> addAccount(type, result, null));
    }

    public static <S extends AccountScope> CompletableFuture<AccountLoginResult> completeLogin(
        AccountType<S> type,
        AccountAuthorizeConfig config,
        AccountConnectionImpl<?> connection,
        Set<String> scopeIds
    ) {
        Set<S> requested = new LinkedHashSet<>();
        for (S scope : type.getAllScopes()) {
            if (scopeIds.contains(scope.getId())) {
                requested.add(scope);
            }
        }
        return type.loadDetails(connection).thenCombine(type.loadGrantedScopes(connection, requested), (details, granted) -> {
            Set<String> grantedIds = new LinkedHashSet<>();
            for (S scope : granted) {
                grantedIds.add(scope.getId());
            }
            return new AccountLoginResult(config, connection, details, grantedIds);
        });
    }

    private CompletableFuture<Account> addAccount(AccountType<?> type, AccountLoginResult result, @Nullable Project project) {
        AccountEntry entry = new AccountEntry();
        entry.ID = UUID.randomUUID().toString();
        fillEntry(entry, type, result);

        AccountImpl account = wrap(entry, AccountLevel.APPLICATION);
        AccountCredentialSource source = findCredentialSource(entry.SOURCE);
        CompletableFuture<?> stored = source == null
            ? CompletableFuture.completedFuture(null)
            : source.store(account, result.connection().serialize()).handle((ignored, error) -> {
                if (error != null) {
                    LOG.error("Failed to store credentials of " + account, error);
                }
                return null;
            });

        return stored.thenApply(ignored -> {
            boolean first;
            synchronized (myLock) {
                first = getAccounts(type, project).isEmpty();
                appState().accounts().add(entry);
            }
            if (first) {
                setActiveAccount(type, account, null);
            }
            fireAccountsChanged();
            return account;
        });
    }

    private CompletableFuture<@Nullable Account> updateAccount(AccountImpl account, AccountLoginResult result) {
        AccountType<?> type = findAccountType(account.getTypeId());
        if (type == null) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (myLock) {
            fillEntry(account.getEntry(), type, result);
        }
        account.setAuthFailed(false);
        myAvatars.remove(account.getId());
        AccountCredentialSource source = findCredentialSource(account.getCredentialSourceId());
        CompletableFuture<?> stored = source == null
            ? CompletableFuture.completedFuture(null)
            : source.store(account, result.connection().serialize());
        return stored.thenApply(ignored -> {
            fireAccountChanged(account);
            return account;
        });
    }

    private void fillEntry(AccountEntry entry, AccountType<?> type, AccountLoginResult result) {
        AccountAuthorizeType<?> authorizeType = result.connection().getAuthorizeType();
        entry.TYPE = type.getId();
        entry.SERVER = StringUtil.notNullize(result.connection().getServerUrl());
        entry.NAME = result.details().getName();
        entry.LOGIN = StringUtil.notNullize(result.details().getLogin());
        entry.AVATAR_URL = StringUtil.notNullize(result.details().getAvatarUrl());
        entry.AUTHORIZE_TYPE = authorizeType.getId();
        entry.SOURCE = authorizeType.getCredentialSourceId();
        entry.SCOPES = new ArrayList<>(result.grantedScopeIds());
    }

    private AccountImpl wrap(AccountEntry entry, AccountLevel level) {
        AccountImpl account = myAccounts.computeIfAbsent(entry.ID, id -> new AccountImpl(entry, level));
        if (account.getEntry() != entry) {
            account = new AccountImpl(entry, level);
            myAccounts.put(entry.ID, account);
        }
        account.setLevel(level);
        return account;
    }

    private AccountListState appState() {
        return myStateHolder.getState();
    }

    private static @Nullable AccountListState projectState(@Nullable Project project) {
        if (project == null || project.isDisposed() || project.isDefault()) {
            return null;
        }
        return project.getInstance(AccountProjectState.class).getState();
    }

    @Override
    public void addListener(AccountListener listener, Disposable parentDisposable) {
        myListeners.add(listener);
        Disposer.register(parentDisposable, () -> myListeners.remove(listener));
    }

    private void fireAccountsChanged() {
        for (AccountListener listener : myListeners) {
            listener.accountsChanged();
        }
    }

    private void fireAccountChanged(Account account) {
        for (AccountListener listener : myListeners) {
            listener.accountChanged(account);
        }
    }
}
