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
package consulo.ai.impl.internal.setting;

import consulo.ai.AIModel;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderType;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.OptionalConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
public class AIProviderConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable, OptionalConfigurable {
    private final Provider<AIProviderTable> myProviderTable;

    @Inject
    public AIProviderConfigurable(Provider<AIProviderTable> providerTable) {
        myProviderTable = providerTable;
    }

    @Override
    public boolean needDisplay() {
        if (Boolean.TRUE)  {
            return false;
        }
        return !myProviderTable.get().getTypes().isEmpty();
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        AIProviderTable providerTable = myProviderTable.get();

        VerticalLayout root = VerticalLayout.create();

        root.add(Label.create(AILocalize.labelInstalledTypes()));
        for (AIProviderType type : providerTable.getTypes()) {
            root.add(Label.create(type.getDisplayName()));
        }

        List<AIProvider> providers = providerTable.getProviders();
        if (providers.isEmpty()) {
            root.add(Label.create(AILocalize.labelNoAccounts()));
            return root;
        }

        for (AIProvider provider : providers) {
            LocalizeValue state = provider.isConfigured()
                ? AILocalize.labelProviderConfigured()
                : AILocalize.labelProviderNotConfigured();
            root.add(DockLayout.create()
                         .left(Label.create(LocalizeValue.of(provider.getName())))
                         .right(Label.create(state)));
        }

        ComboBox<AIProvider> providerBox = ComboBox.create(providers);
        providerBox.setTextRenderer(provider -> provider == null ? LocalizeValue.empty() : LocalizeValue.of(provider.getName()));
        providerBox.setValue(providerTable.getDefaultProvider());
        root.add(DockLayout.create().left(Label.create(AILocalize.labelDefaultProvider())).right(providerBox));

        AIProvider defaultProvider = providerTable.getDefaultProvider();
        List<AIModel> models = defaultProvider == null ? List.of() : defaultProvider.getModels();
        if (!models.isEmpty()) {
            ComboBox<AIModel> modelBox = ComboBox.create(models);
            // the renderer is handed a null when nothing is selected
            modelBox.setTextRenderer(model -> model == null ? LocalizeValue.empty() : model.getDisplayName());
            modelBox.setValue(providerTable.getDefaultModel());

            propertyBuilder.add(modelBox,
                                providerTable::getDefaultModel,
                                model -> providerTable.setDefault(providerBox.getValue(), model));

            root.add(DockLayout.create().left(Label.create(AILocalize.labelDefaultModel())).right(modelBox));
        }

        return root;
    }

    @Override
    public String getId() {
        return "ai.providers";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.configurableDisplayName();
    }
}
