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
package consulo.language.editor.impl.internal.intention.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtensionImpl
public class IntentionSettingsConfigurable implements ApplicationConfigurable, SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    private final Application myApplication;

    private @Nullable IntentionSettingsPanel myPanel;

    @Inject
    public IntentionSettingsConfigurable(Application application) {
        myApplication = application;
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable uiDisposable) {
        IntentionSettingsPanel panel = myPanel;
        if (panel == null) {
            panel = new IntentionSettingsPanel(myApplication);
            Disposer.register(uiDisposable, panel);
            myPanel = panel;
        }
        return panel.getComponent();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        IntentionSettingsPanel panel = myPanel;
        return panel == null ? null : panel.getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        IntentionSettingsPanel panel = myPanel;
        return panel != null && panel.isModified();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return CodeInsightLocalize.intentionSettings();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        IntentionSettingsPanel panel = myPanel;
        if (panel != null) {
            panel.reset();
        }
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        IntentionSettingsPanel panel = myPanel;
        if (panel != null) {
            panel.apply();
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myPanel = null;
    }

    @Override
    public @Nullable Runnable enableSearch(String option) {
        IntentionSettingsPanel panel = myPanel;
        return panel == null ? null : () -> panel.showOption(option);
    }

    @Override
    public String getId() {
        return "editor.code.intentions";
    }

    @RequiredUIAccess
    public void selectIntention(LocalizeValue actionText) {
        IntentionSettingsPanel panel = myPanel;
        if (panel != null) {
            panel.selectIntention(actionText);
        }
    }
}
