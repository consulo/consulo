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

import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderType;
import consulo.ai.impl.internal.setting.AISettings;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@Singleton
@ServiceImpl
public class AIProviderTableImpl implements AIProviderTable {
    private final Application myApplication;
    private final Provider<AISettings> mySettings;

    @Inject
    public AIProviderTableImpl(Application application, Provider<AISettings> settings) {
        myApplication = application;
        mySettings = settings;
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

    @Override
    public List<AIProvider> getProviders() {
        List<AIProvider> providers = new ArrayList<>();

        for (AISettings.ProviderEntry entry : mySettings.get().PROVIDERS) {
            // the plugin that supplied the type can be gone, in which case the entry is simply skipped
            AIProviderType type = findType(entry.TYPE_ID);
            if (type != null) {
                providers.add(new AIProvider(entry.NAME, type, entry.OPTIONS));
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

        mySettings.get().PROVIDERS.add(new AISettings.ProviderEntry(name, type.getId()));
        return new AIProvider(name, type);
    }

    @Override
    public CompletableFuture<?> removeProvider(AIProvider provider) {
        mySettings.get().PROVIDERS.removeIf(entry -> entry.NAME.equals(provider.getName()));

        if (provider.getName().equals(mySettings.get().DEFAULT_PROVIDER_NAME)) {
            setDefault(null, null);
        }

        // the key would otherwise outlive the instance it belongs to
        return provider.setApiKey(null);
    }

    @Override
    public CompletableFuture<@Nullable AIProvider> getDefaultProvider() {
        AIProvider chosen = findProvider(mySettings.get().DEFAULT_PROVIDER_NAME);
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
        String modelId = mySettings.get().DEFAULT_MODEL_ID;
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
        AISettings settings = mySettings.get();
        settings.DEFAULT_PROVIDER_NAME = provider == null ? "" : provider.getName();
        settings.DEFAULT_MODEL_ID = model == null ? "" : model.getId();
    }
}
