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
                providers.add(new AIProvider(entry.NAME, type));
            }
        }
        return providers;
    }

    @Override
    public List<AIProvider> getConfiguredProviders() {
        List<AIProvider> configured = new ArrayList<>();
        for (AIProvider provider : getProviders()) {
            if (provider.isConfigured()) {
                configured.add(provider);
            }
        }
        return configured;
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
    public void removeProvider(AIProvider provider) {
        mySettings.get().PROVIDERS.removeIf(entry -> entry.NAME.equals(provider.getName()));

        // the key would otherwise outlive the instance it belongs to
        provider.setApiKey(null);

        if (provider.getName().equals(mySettings.get().DEFAULT_PROVIDER_NAME)) {
            setDefault(null, null);
        }
    }

    @Override
    public @Nullable AIProvider getDefaultProvider() {
        AIProvider chosen = findProvider(mySettings.get().DEFAULT_PROVIDER_NAME);
        if (chosen != null) {
            return chosen;
        }

        List<AIProvider> configured = getConfiguredProviders();
        return configured.isEmpty() ? null : configured.getFirst();
    }

    @Override
    public @Nullable AIModel getDefaultModel() {
        AIProvider provider = getDefaultProvider();
        if (provider == null) {
            return null;
        }

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
