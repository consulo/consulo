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

import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

/**
 * The rules every agent follows - sent, hidden, in front of each prompt of every chat.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIUserRulesConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    private static final int VISIBLE_ROWS = 12;

    private final Provider<AISettings> mySettings;

    @Inject
    public AIUserRulesConfigurable(Provider<AISettings> settings) {
        mySettings = settings;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        AISettings settings = mySettings.get();
        VerticalLayout root = VerticalLayout.create();

        CheckBox enabled = CheckBox.create(AILocalize.checkboxUserRulesEnabled());
        propertyBuilder.add(enabled, settings::isUserRulesEnabled, settings::setUserRulesEnabled);
        root.add(enabled);

        TextArea rules = TextArea.create();
        rules.setMinRows(VISIBLE_ROWS);
        propertyBuilder.add(rules, settings::getUserRules, settings::setUserRules);
        root.add(rules);

        Label hint = Label.create(AILocalize.labelUserRulesHint());
        hint.addStyle(LabelStyle.FONT_SMALL);
        root.add(hint);
        return root;
    }

    @Override
    public String getId() {
        return "ai.user.rules";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.configurableUserRules();
    }
}
