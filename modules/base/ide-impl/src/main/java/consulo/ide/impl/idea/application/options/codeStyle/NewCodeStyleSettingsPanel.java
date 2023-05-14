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
import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractConfigurable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.logging.Logger;

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

  public JComponent getPanel(@Nonnull Disposable uiDisposable) {
    if (myPanel.getComponentCount() == 0) {
      JComponent component = myTab.createComponent(uiDisposable);
      myPanel.add(component, BorderLayout.CENTER);
    }
    return myPanel;
  }

  public boolean isModified() {
    return myTab.isModified();
  }

  public void updatePreview() {
    if (myTab instanceof CodeStyleAbstractConfigurable configurable) {
      configurable.onSomethingChanged();
    }
  }

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

  public void dispose() {
    myTab.disposeUIResources();
  }

  public void reset() {
    myTab.reset();
    updatePreview();
  }

  public String getDisplayName() {
    return myTab.getDisplayName();
  }

  public void setModel(final CodeStyleSchemesModel model) {
    if (myTab instanceof CodeStyleAbstractConfigurable) {
      ((CodeStyleAbstractConfigurable)myTab).setModel(model);
    }
  }
  public void onSomethingChanged() {
    if (myTab instanceof CodeStyleAbstractConfigurable) {
      ((CodeStyleAbstractConfigurable)myTab).onSomethingChanged();
    }
  }

  public Set<String> processListOptions() {
    if (myTab instanceof OptionsContainingConfigurable) {
      return ((OptionsContainingConfigurable) myTab).processListOptions();
    }
    return Collections.emptySet();
  }

  @Nullable
  public CodeStyleAbstractPanel getSelectedPanel() {
    if (myTab instanceof CodeStyleAbstractConfigurable) {
      return ((CodeStyleAbstractConfigurable)myTab).getPanel();
    }
    return null;
  }

  @Override
  public void tabChanged(@Nonnull TabbedLanguageCodeStylePanel source, @Nonnull String tabTitle) {
    CodeStyleAbstractPanel panel = getSelectedPanel();
    if (panel instanceof TabbedLanguageCodeStylePanel && panel != source) {
      ((TabbedLanguageCodeStylePanel)panel).changeTab(tabTitle);
    }
  }
}
