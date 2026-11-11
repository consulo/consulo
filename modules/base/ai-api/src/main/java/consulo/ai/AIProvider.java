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
package consulo.ai;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountManager;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * One configured account of an {@link AIProviderType} - the analogue of a single configured SDK.
 * Carries a user-visible name and exactly one set of credentials, so a user can keep, say, a work and
 * a personal Claude account side by side.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public final class AIProvider {
    /**
     * The option holding the id of the {@linkplain consulo.accountManagement.Account account} an instance signs in
     * with, for a type which {@linkplain AIProviderType#getAccountTypeId() signs in through accounts}.
     */
    public static final String ACCOUNT_OPTION = "account";

    private final String myName;
    private final AIProviderType myType;
    private final Map<String, String> myOptions;

    public AIProvider(String name, AIProviderType type) {
        this(name, type, Map.of());
    }

    /**
     * @param options settings of the account its type keeps - see {@link AIProviderType#createAccountComponent}
     */
    public AIProvider(String name, AIProviderType type, Map<String, String> options) {
        myName = name;
        myType = type;
        myOptions = Map.copyOf(options);
    }

    /**
     * Unique among configured instances, and the key the API key is stored under.
     */
    public String getName() {
        return myName;
    }

    public AIProviderType getType() {
        return myType;
    }

    public Map<String, String> getOptions() {
        return myOptions;
    }

    public @Nullable String getOption(String key) {
        return myOptions.get(key);
    }

    public List<AIModel> getModels() {
        return myType.getModels();
    }

    public CompletableFuture<List<AIModel>> loadModels() {
        return myType.loadModels();
    }

    /**
     * The id of the account the instance signs in with, null when none is chosen.
     */
    public @Nullable String getAccountId() {
        String id = myOptions.get(ACCOUNT_OPTION);
        return id == null || id.isEmpty() ? null : id;
    }

    public @Nullable Account findAccount() {
        String id = getAccountId();
        return id == null ? null : accountManager().findAccount(id, null);
    }

    /**
     * Connects with the account of the instance - its credentials, to decorate a request with.
     */
    public CompletableFuture<AccountConnection> connectAccount() {
        Account account = findAccount();
        if (account == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The AI account " + myName + " has no account to sign in with"));
        }
        return accountManager().connect(account);
    }

    private static AccountManager accountManager() {
        return Application.get().getInstance(AccountManager.class);
    }

    public CompletableFuture<@Nullable String> getApiKey() {
        return AICredentials.getApiKey(myName);
    }

    public CompletableFuture<?> setApiKey(@Nullable String apiKey) {
        return AICredentials.setApiKey(myName, apiKey);
    }

    /**
     * Whether this instance can answer. The UI uses it to tell "no key yet" apart from "failing".
     */
    public CompletableFuture<Boolean> isConfigured() {
        return myType.isConfigured(this);
    }

    public CompletableFuture<AIResponse> chat(AIRequest request, @Nullable AIStreamListener listener) {
        return myType.chat(this, request, listener);
    }

    public CompletableFuture<AIResponse> chat(AIRequest request) {
        return chat(request, null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof AIProvider provider && myName.equals(provider.myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public String toString() {
        return myName + " (" + myType.getId() + ")";
    }
}
