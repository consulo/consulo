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
package consulo.ai.impl.internal;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderType;
import consulo.ai.impl.internal.setting.AISettings;
import consulo.ai.impl.internal.setting.state.AISettingsState;
import consulo.ai.impl.internal.setting.state.ProviderEntry;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@Singleton
@ServiceImpl
public class AIProviderTableImpl implements AIProviderTable {
    private static final Logger LOG = Logger.getInstance(AIProviderTableImpl.class);

    private final Application myApplication;
    private final Provider<AccountManager> myAccountManager;
    private final Provider<AISettings> mySettings;
    /**
     * Instances asked to move to an account during this run - asked once, whatever they answer.
     */
    private final Set<String> myMigrated = ConcurrentHashMap.newKeySet();
    /**
     * How many instances are moving to an account right now - the accounts they add are theirs, not new ones.
     */
    private final AtomicInteger myMigrating = new AtomicInteger();
    private final AtomicBoolean myWatchingAccounts = new AtomicBoolean();
    /**
     * The accounts known when they were last looked at - an account not among them was added since.
     */
    private final Set<String> myKnownAccounts = ConcurrentHashMap.newKeySet();

    @Inject
    public AIProviderTableImpl(Application application, Provider<AISettings> settings, Provider<AccountManager> accountManager) {
        myApplication = application;
        mySettings = settings;
        myAccountManager = accountManager;
    }

    @Override
    public List<AIProviderType> getTypes() {
        List<AIProviderType> types = new ArrayList<>();
        myApplication.getExtensionPoint(AIProviderType.class).forEach(types::add);
        return types;
    }

    @Override
    public @Nullable AIProviderType findType(String typeId) {
        for (AIProviderType type : getTypes()) {
            if (type.getId().equals(typeId)) {
                return type;
            }
        }
        return null;
    }

    /**
     * An instance of a type which signs in through accounts but has no account yet signed in the way it did before -
     * its type moves it, and the key it kept in the password safe goes once it is moved.
     */
    private void migrateToAccount(ProviderEntry entry, AIProvider provider) {
        AIProviderType type = provider.getType();
        if (type.getAccountTypeId() == null || provider.getAccountId() != null || !myMigrated.add(provider.getName())) {
            return;
        }
        myMigrating.incrementAndGet();
        provider.getApiKey()
            .thenCompose(secret -> type.migrateToAccount(provider, secret).thenApply(accountId -> {
                if (accountId != null) {
                    Map<String, String> options = new LinkedHashMap<>(entry.OPTIONS);
                    options.put(AIProvider.ACCOUNT_OPTION, accountId);
                    entry.OPTIONS = options;
                    if (secret != null) {
                        provider.setApiKey(null);
                    }
                }
                return accountId;
            }))
            .whenComplete((accountId, error) -> {
                myMigrating.decrementAndGet();
                if (error != null) {
                    LOG.warn("Cannot move the AI account " + provider.getName() + " to an account", error);
                }
            });
    }

    /**
     * Starts watching the accounts once: an account added since, of a type an AI provider type signs in with, gets an
     * instance of its own unless one uses it already.
     */
    private void watchAccounts() {
        if (!myWatchingAccounts.compareAndSet(false, true)) {
            return;
        }
        AccountManager accountManager = myAccountManager.get();
        for (Account account : accountManager.getAccounts(null)) {
            myKnownAccounts.add(account.getId());
        }
        accountManager.addListener(new AccountListener() {
            @Override
            public void accountsChanged() {
                removeProvidersOfRemovedAccounts(accountManager);
                addProvidersOfNewAccounts(accountManager);
            }
        }, myApplication);
    }

    /**
     * The instances added for an account go away with it - the ones the user changed since stay, with the account
     * missing.
     */
    private void removeProvidersOfRemovedAccounts(AccountManager accountManager) {
        Set<String> current = new HashSet<>();
        for (Account account : accountManager.getAccounts(null)) {
            current.add(account.getId());
        }
        Set<String> removed = new HashSet<>();
        for (String known : myKnownAccounts) {
            if (!current.contains(known)) {
                removed.add(known);
            }
        }
        if (removed.isEmpty()) {
            return;
        }
        myKnownAccounts.removeAll(removed);

        AISettingsState settings = mySettings.get().getState();
        List<ProviderEntry> generated = new ArrayList<>();
        for (ProviderEntry entry : settings.PROVIDERS) {
            if (entry.GENERATED && removed.contains(entry.OPTIONS.get(AIProvider.ACCOUNT_OPTION))) {
                generated.add(entry);
            }
        }
        for (ProviderEntry entry : generated) {
            settings.PROVIDERS.remove(entry);
            if (entry.NAME.equals(settings.DEFAULT_PROVIDER_NAME)) {
                setDefault(null, null);
            }
        }
    }

    private void addProvidersOfNewAccounts(AccountManager accountManager) {
        List<Account> added = new ArrayList<>();
        for (Account account : accountManager.getAccounts(null)) {
            if (myKnownAccounts.add(account.getId())) {
                added.add(account);
            }
        }
        // an account an instance moved to is that instance's
        if (added.isEmpty() || myMigrating.get() > 0) {
            return;
        }

        Set<String> used = new HashSet<>();
        for (AIProvider provider : getProviders()) {
            String accountId = provider.getAccountId();
            if (accountId != null) {
                used.add(accountId);
            }
        }
        for (Account account : added) {
            if (used.contains(account.getId())) {
                continue;
            }
            for (AIProviderType type : getTypes()) {
                if (account.getTypeId().equals(type.getAccountTypeId())) {
                    addProvider(type, account);
                    break;
                }
            }
        }
    }

    private void addProvider(AIProviderType type, Account account) {
        String base = AILocalize.providerAutoName(type.getDisplayName(), account.getName()).get();
        String name = base;
        for (int i = 2; findProvider(name) != null; i++) {
            name = base + " " + i;
        }
        ProviderEntry entry = new ProviderEntry(name, type.getId());
        entry.OPTIONS.put(AIProvider.ACCOUNT_OPTION, account.getId());
        entry.GENERATED = true;
        mySettings.get().getState().PROVIDERS.add(entry);
    }

    @Override
    public List<AIProvider> getProviders() {
        watchAccounts();
        List<AIProvider> providers = new ArrayList<>();

        for (ProviderEntry entry : mySettings.get().getState().PROVIDERS) {
            // the plugin that supplied the type can be gone, in which case the entry is simply skipped
            AIProviderType type = findType(entry.TYPE_ID);
            if (type != null) {
                AIProvider provider = new AIProvider(entry.NAME, type, entry.OPTIONS);
                migrateToAccount(entry, provider);
                providers.add(provider);
            }
        }
        return providers;
    }

    @Override
    public CompletableFuture<List<AIProvider>> getConfiguredProviders() {
        List<AIProvider> providers = getProviders();
        List<CompletableFuture<Boolean>> checks = new ArrayList<>(providers.size());
        for (AIProvider provider : providers) {
            checks.add(provider.isConfigured());
        }

        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<AIProvider> configured = new ArrayList<>();
            for (int i = 0; i < providers.size(); i++) {
                if (checks.get(i).join()) {
                    configured.add(providers.get(i));
                }
            }
            return configured;
        });
    }

    @Override
    public @Nullable AIProvider findProvider(String name) {
        for (AIProvider provider : getProviders()) {
            if (provider.getName().equals(name)) {
                return provider;
            }
        }
        return null;
    }

    @Override
    public AIProvider addProvider(String name, AIProviderType type) {
        if (findProvider(name) != null) {
            throw new IllegalArgumentException("An AI provider named '" + name + "' already exists");
        }

        mySettings.get().getState().PROVIDERS.add(new ProviderEntry(name, type.getId()));
        return new AIProvider(name, type);
    }

    @Override
    public CompletableFuture<?> removeProvider(AIProvider provider) {
        mySettings.get().getState().PROVIDERS.removeIf(entry -> entry.NAME.equals(provider.getName()));

        if (provider.getName().equals(mySettings.get().getState().DEFAULT_PROVIDER_NAME)) {
            setDefault(null, null);
        }

        // the key would otherwise outlive the instance it belongs to
        return provider.setApiKey(null);
    }

    @Override
    public CompletableFuture<@Nullable AIProvider> getDefaultProvider() {
        AIProvider chosen = findProvider(mySettings.get().getState().DEFAULT_PROVIDER_NAME);
        if (chosen != null) {
            return CompletableFuture.completedFuture(chosen);
        }

        return getConfiguredProviders().thenApply(configured -> configured.isEmpty() ? null : configured.getFirst());
    }

    @Override
    public CompletableFuture<@Nullable AIModel> getDefaultModel() {
        return getDefaultProvider().thenApply(provider -> provider == null ? null : defaultModel(provider));
    }

    private @Nullable AIModel defaultModel(AIProvider provider) {
        String modelId = mySettings.get().getState().DEFAULT_MODEL_ID;
        for (AIModel model : provider.getModels()) {
            if (model.getId().equals(modelId)) {
                return model;
            }
        }

        List<AIModel> models = provider.getModels();
        return models.isEmpty() ? null : models.getFirst();
    }

    @Override
    public void setDefault(@Nullable AIProvider provider, @Nullable AIModel model) {
        AISettingsState settings = mySettings.get().getState();
        settings.DEFAULT_PROVIDER_NAME = provider == null ? "" : provider.getName();
        settings.DEFAULT_MODEL_ID = model == null ? "" : model.getId();
    }
}
