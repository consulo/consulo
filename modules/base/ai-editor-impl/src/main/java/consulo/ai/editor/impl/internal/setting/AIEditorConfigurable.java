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
package consulo.ai.editor.impl.internal.setting;

import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-15
 */
@ExtensionImpl
public class AIEditorConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    private final Provider<AIEditorSettings> mySettings;

    @Inject
    public AIEditorConfigurable(Provider<AIEditorSettings> settings) {
        mySettings = settings;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        AIEditorSettings settings = mySettings.get();
        VerticalLayout root = VerticalLayout.create();

        CheckBox showGroup = CheckBox.create(AIEditorLocalize.checkboxShowEditorPopupGroup());
        propertyBuilder.add(showGroup, settings::isShowEditorPopupGroup, settings::setShowEditorPopupGroup);
        root.add(showGroup);
        return root;
    }

    @Override
    public String getId() {
        return "ai.editor";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AIEditorLocalize.configurableEditor();
    }
}
