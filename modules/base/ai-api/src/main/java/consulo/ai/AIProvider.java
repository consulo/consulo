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

import org.jspecify.annotations.Nullable;

import java.util.List;
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
    private final String myName;
    private final AIProviderType myType;

    public AIProvider(String name, AIProviderType type) {
        myName = name;
        myType = type;
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

    public List<AIModel> getModels() {
        return myType.getModels();
    }

    public @Nullable String getApiKey() {
        return AICredentials.getApiKey(myName);
    }

    public void setApiKey(@Nullable String apiKey) {
        AICredentials.setApiKey(myName, apiKey);
    }

    /**
     * Whether this instance can answer. The UI uses it to tell "no key yet" apart from "failing".
     */
    public boolean isConfigured() {
        return getApiKey() != null;
    }

    public CompletableFuture<AIResponse> chat(AIRequest request, @Nullable AIStreamListener listener) {
        return myType.chat(this, request, listener);
    }

    public CompletableFuture<AIResponse> chat(AIRequest request) {
        return chat(request, null);
    }

    @Override
    public boolean equals(Object other) {
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
