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
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.localize.AILocalize;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.PasswordBox;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The details of one account: its name, the key when its type needs one, and whether it is the default.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
class AIProviderDetailsConfigurable implements MasterDetailsConfigurable<AIProviderDraft> {
    private final AIProviderDraft myDraft;
    private final AIProviderTable myProviderTable;
    private final Runnable myUpdateTree;

    private @Nullable TextBox myNameBox;
    private @Nullable PasswordBox myApiKeyBox;
    private @Nullable CheckBox myDefaultBox;
    private @Nullable ComboBox<AIModel> myModelBox;

    AIProviderDetailsConfigurable(AIProviderDraft draft, AIProviderTable providerTable, Runnable updateTree) {
        myDraft = draft;
        myProviderTable = providerTable;
        myUpdateTree = updateTree;
    }

    @Override
    public @Nullable Image getIcon() {
        return myDraft.type.getIcon();
    }

    @Override
    public AIProviderDraft getEditableObject() {
        return myDraft;
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
        TextBox nameBox = myNameBox;
        if (nameBox != null) {
            nameBox.setValue(myDraft.name);
        }
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return AILocalize.bannerAccount(myDraft.type.getDisplayName(), myDraft.name);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myDraft.name);
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        FormBuilder builder = FormBuilder.create();

        TextBox nameBox = TextBox.create();
        nameBox.addValueListener(event -> {
            String name = StringUtil.notNullize(event.getValue()).trim();
            if (!name.isEmpty()) {
                myDraft.name = name;
                myUpdateTree.run();
            }
        });
        builder.addLabeled(AILocalize.labelName(), nameBox);
        builder.addLabeled(AILocalize.labelType(), Label.create(myDraft.type.getDisplayName()));

        if (myDraft.type.acceptsApiKey()) {
            PasswordBox apiKeyBox = PasswordBox.create();
            LocalizeValue label = myDraft.type.getApiKeyLabel();
            builder.addLabeled(label == LocalizeValue.empty() ? AILocalize.labelApiKey() : label, apiKeyBox);
            myApiKeyBox = apiKeyBox;
        }

        List<AIModel> models = myDraft.type.getModels();
        ComboBox<AIModel> modelBox = ComboBox.create(models);
        modelBox.setTextRenderer(model -> model == null ? LocalizeValue.empty() : model.getDisplayName());
        builder.addLabeled(AILocalize.labelDefaultModelField(), modelBox);

        CheckBox defaultBox = CheckBox.create(AILocalize.labelUseAsDefault());
        builder.addBottom(defaultBox);

        Component accountComponent = myDraft.type.createAccountComponent(myDraft.options, parentDisposable);
        if (accountComponent != null) {
            builder.addBottom(accountComponent);
        }

        myNameBox = nameBox;
        myModelBox = modelBox;
        myDefaultBox = defaultBox;

        reset();
        return builder.build();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        TextBox nameBox = myNameBox;
        PasswordBox apiKeyBox = myApiKeyBox;
        CheckBox defaultBox = myDefaultBox;
        ComboBox<AIModel> modelBox = myModelBox;
        return nameBox != null && !StringUtil.notNullize(nameBox.getValue()).trim().equals(myDraft.name)
            || apiKeyBox != null && myDraft.apiKey != null && !StringUtil.notNullize(apiKeyBox.getValue()).equals(myDraft.apiKey)
            || defaultBox != null && defaultBox.getValueOrError() != myDraft.isDefault
            || modelBox != null && !Objects.equals(modelId(modelBox.getValue()), myDraft.defaultModelId);
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        TextBox nameBox = myNameBox;
        if (nameBox != null) {
            String name = StringUtil.notNullize(nameBox.getValue()).trim();
            if (name.isEmpty()) {
                throw new ConfigurationException(AILocalize.errorNameRequired());
            }
            myDraft.name = name;
        }

        PasswordBox apiKeyBox = myApiKeyBox;
        if (apiKeyBox != null) {
            String key = StringUtil.notNullize(apiKeyBox.getValue());
            if (!key.equals(StringUtil.notNullize(myDraft.apiKey))) {
                myDraft.apiKey = key;
                myDraft.apiKeyChanged = true;
            }
        }

        CheckBox defaultBox = myDefaultBox;
        ComboBox<AIModel> modelBox = myModelBox;
        myDraft.isDefault = defaultBox != null && defaultBox.getValueOrError();
        myDraft.defaultModelId = modelBox == null ? null : modelId(modelBox.getValue());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        TextBox nameBox = myNameBox;
        if (nameBox != null) {
            nameBox.setValue(myDraft.name);
        }

        CheckBox defaultBox = myDefaultBox;
        if (defaultBox != null) {
            defaultBox.setValue(myDraft.isDefault);
        }

        ComboBox<AIModel> modelBox = myModelBox;
        if (modelBox != null) {
            String modelId = myDraft.defaultModelId;
            AIModel selected = null;
            for (AIModel model : myDraft.type.getModels()) {
                if (model.getId().equals(modelId)) {
                    selected = model;
                }
            }
            if (selected == null && !myDraft.type.getModels().isEmpty()) {
                selected = myDraft.type.getModels().getFirst();
            }
            modelBox.setValue(selected);
            if (myDraft.defaultModelId == null) {
                myDraft.defaultModelId = modelId(selected);
            }
        }

        PasswordBox apiKeyBox = myApiKeyBox;
        if (apiKeyBox == null) {
            return;
        }
        if (myDraft.apiKey != null || myDraft.originalName == null) {
            apiKeyBox.setValue(StringUtil.notNullize(myDraft.apiKey));
            return;
        }
        AIProvider stored = myProviderTable.findProvider(myDraft.originalName);
        if (stored == null) {
            return;
        }
        stored.getApiKey().whenCompleteAsync((key, error) -> {
            myDraft.apiKey = StringUtil.notNullize(key);
            apiKeyBox.setValue(myDraft.apiKey);
        }, UIAccess.current());
    }

    private static @Nullable String modelId(@Nullable AIModel model) {
        return model == null ? null : model.getId();
    }
}
