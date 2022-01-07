/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.plugins;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-06-26
 */
public class PluginsConfigurable implements SearchableConfigurable, Configurable.NoScroll, Configurable.HoldPreferredFocusedComponent, Configurable.NoMargin {
  public static final String ID = "platformAndPlugins";

  private PluginsPanel myPanel;

  @Inject
  public PluginsConfigurable() {
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable parentDisposable) {
    if (myPanel == null) {
      myPanel = new PluginsPanel();
      Disposer.register(parentDisposable, myPanel);
    }
    return myPanel.getComponent();
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.plugins");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    String suffix = "";
    if (myPanel != null) {
      int selectedIndex = myPanel.getSelectedIndex();

      switch (selectedIndex) {
        case PluginsPanel.INSTALLED:
          suffix = "/#installed";
          break;
        case PluginsPanel.FROM_REPOSITORY:
          suffix = "/#from-repository";
          break;
      }
    }
    return getId() + suffix;
  }

  @Nonnull
  @Override
  public String getId() {
    return ID;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myPanel != null) {
      myPanel.apply();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myPanel != null) {
      myPanel.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myPanel = null;
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return () -> {
      if (myPanel != null) myPanel.filter(option);
    };
  }

  public void selectInstalled(PluginId pluginId) {
    myPanel.selectInstalled(pluginId);
  }

  public void selectAvailable(PluginId pluginId) {
    myPanel.selectAvailable(pluginId);
  }

  public void select(PluginId pluginId) {
    myPanel.getSelected().select(pluginId);
  }
}
