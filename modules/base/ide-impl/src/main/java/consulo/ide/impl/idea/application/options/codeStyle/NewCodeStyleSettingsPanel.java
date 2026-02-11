/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.OptionsContainingConfigurable;
import consulo.disposer.Disposable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractConfigurable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

/**
 * @author max
 */
public class NewCodeStyleSettingsPanel implements TabbedLanguageCodeStylePanel.TabChangeListener {
    private static final Logger LOG = Logger.getInstance(NewCodeStyleSettingsPanel.class);

    private final JPanel myPanel;
    private final Configurable myTab;

    public NewCodeStyleSettingsPanel(Configurable tab) {
        myPanel = new JPanel(new BorderLayout());
        myTab = tab;
    }

    @RequiredUIAccess
    public JComponent getPanel(@Nonnull Disposable uiDisposable) {
        if (myPanel.getComponentCount() == 0) {
            JComponent component = myTab.createComponent(uiDisposable);
            myPanel.add(component, BorderLayout.CENTER);
        }
        return myPanel;
    }

    @RequiredUIAccess
    public boolean isModified() {
        return myTab.isModified();
    }

    @RequiredUIAccess
    public void updatePreview() {
        if (myTab instanceof CodeStyleAbstractConfigurable configurable) {
            configurable.onSomethingChanged();
        }
    }

    @RequiredUIAccess
    public void apply() {
        try {
            if (myTab.isModified()) {
                myTab.apply();
            }
        }
        catch (ConfigurationException e) {
            LOG.error(e);
        }
    }

    @Nullable
    public String getHelpTopic() {
        return myTab.getHelpTopic();
    }

    @RequiredUIAccess
    public void dispose() {
        myTab.disposeUIResources();
    }

    @RequiredUIAccess
    public void reset() {
        myTab.reset();
        updatePreview();
    }

    @Nonnull
    public LocalizeValue getDisplayName() {
        return myTab.getDisplayName();
    }

    public void setModel(CodeStyleSchemesModel model) {
        if (myTab instanceof CodeStyleAbstractConfigurable codeStyleAbstractConfigurable) {
            codeStyleAbstractConfigurable.setModel(model);
        }
    }

    public void onSomethingChanged() {
        if (myTab instanceof CodeStyleAbstractConfigurable codeStyleAbstractConfigurable) {
            codeStyleAbstractConfigurable.onSomethingChanged();
        }
    }

    public Set<String> processListOptions() {
        if (myTab instanceof OptionsContainingConfigurable optionsContainingConfigurable) {
            return optionsContainingConfigurable.processListOptions();
        }
        return Collections.emptySet();
    }

    @Nullable
    public CodeStyleAbstractPanel getSelectedPanel() {
        if (myTab instanceof CodeStyleAbstractConfigurable codeStyleAbstractConfigurable) {
            return codeStyleAbstractConfigurable.getPanel();
        }
        return null;
    }

    @Override
    public void tabChanged(@Nonnull TabbedLanguageCodeStylePanel source, @Nonnull String tabTitle) {
        CodeStyleAbstractPanel panel = getSelectedPanel();
        if (panel instanceof TabbedLanguageCodeStylePanel tabbedLanguageCodeStylePanel && panel != source) {
            tabbedLanguageCodeStylePanel.changeTab(tabTitle);
        }
    }
}
