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
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.TypeExtension;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A kind of AI backend - Claude, ChatGPT, a local runtime - in the same sense that
 * {@link consulo.content.bundle.SdkType} is a kind of SDK. The type knows the vendor's models and how
 * to talk to it; the credentials live on the {@link AIProvider} instances the user configures.
 * <p>
 * Implementations live in plugins, so the platform never depends on any particular vendor.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIProviderType extends TypeExtension {
    /**
     * Stable identifier, for example {@code anthropic}. Persisted against every configured instance,
     * so it must not change between releases.
     */
    @Override
    String getId();

    @Override
    LocalizeValue getDisplayName();

    @Override
    Image getIcon();

    /**
     * The vendor's models. Declared by the type rather than fetched, so they are known before any
     * instance is configured.
     */
    List<AIModel> getModels();

    /**
     * The models once the vendor told them - {@link #getModels()} gives the ones known so far, a type which asks the
     * vendor completes this when it answered.
     */
    default CompletableFuture<List<AIModel>> loadModels() {
        return CompletableFuture.completedFuture(getModels());
    }

    /**
     * The model of the known ones a kept {@linkplain AIModel#getId() id} stands for - the one with that id, unless the
     * type knows the id under another name, as an alias of the vendor.
     */
    default @Nullable AIModel resolveModel(List<AIModel> models, String id) {
        for (AIModel model : models) {
            if (model.getId().equals(id)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Sends a request using the given instance's credentials.
     * <p>
     * When {@code listener} is given and the model reports {@link AIModelCapability#STREAMING},
     * fragments are delivered as they arrive; the returned future still completes with the assembled
     * answer, so callers never reassemble it themselves. Cancelling the future should abort the request.
     */
    CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener);

    /**
     * The {@linkplain consulo.accountManagement.AccountType account type} an instance signs in with - the settings then
     * choose an account of it instead of asking for an API key, and {@link AIProvider#connectAccount()} gives its
     * credentials. Null for a type which keeps an API key of its own.
     */
    default @Nullable String getAccountTypeId() {
        return null;
    }

    /**
     * Moves an instance which signed in before accounts existed to an account - called once for an instance of a type
     * with an {@linkplain #getAccountTypeId() account type} which has none chosen yet.
     *
     * @param legacySecret the API key the instance kept in the password safe, if any
     * @return the id of the account the instance signs in with now, null when there is nothing to move
     */
    default CompletableFuture<@Nullable String> migrateToAccount(AIProvider provider, @Nullable String legacySecret) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Whether an instance is configured with an API key the settings ask for. A type which signs in some other way
     * returns false and overrides {@link #isConfigured}.
     */
    default boolean requiresApiKey() {
        return true;
    }

    /**
     * Whether the settings offer an API key at all - also when it is optional, as for a type which can sign in some
     * other way but takes a key instead.
     */
    default boolean acceptsApiKey() {
        return requiresApiKey();
    }

    /**
     * What the settings call the secret of an account, when {@link #acceptsApiKey()} - empty for "API key".
     */
    default LocalizeValue getApiKeyLabel() {
        return LocalizeValue.empty();
    }

    /**
     * Settings of the type's own shown with every account of it - how it signs in, for example. The component edits
     * {@code options}, the options of the account being edited, which the settings store with the account on apply
     * and hand back through {@link AIProvider#getOptions()}. Null when the type has none.
     */
    @RequiredUIAccess
    default @Nullable Component createAccountComponent(Map<String, String> options, Disposable uiDisposable) {
        return null;
    }

    /**
     * Whether the instance can answer. By default it needs an API key; a type which signs in some other way - a
     * command line tool with a login of its own - answers for itself.
     */
    default CompletableFuture<Boolean> isConfigured(AIProvider provider) {
        if (getAccountTypeId() != null) {
            Account account = provider.findAccount();
            return CompletableFuture.completedFuture(account != null && !account.isAuthFailed());
        }
        return provider.getApiKey().thenApply(apiKey -> apiKey != null);
    }

    /**
     * The conversation with the {@linkplain AIRequest#getSessionId() id} ended; a type which keeps state for it lets
     * it go.
     */
    default void closeSession(AIProvider provider, String sessionId) {
    }
}
