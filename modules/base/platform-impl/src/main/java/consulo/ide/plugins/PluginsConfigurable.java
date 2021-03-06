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
import consulo.container.plugin.PluginDescriptor;
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
  public static final String ID = "preferences.pluginManager";

  private PluginsPanel myPanel;

  @Inject
  public PluginsConfigurable() {
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new PluginsPanel();
    }
    return myPanel.getComponent();
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.plugins");
  }

  @Nonnull
  @Override
  public String getId() {
    return "preferences.pluginManager";
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
    if (myPanel != null) {
      Disposer.dispose(myPanel);
    }
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return () -> {
      if (myPanel != null) myPanel.filter(option);
    };
  }

  public void select(PluginDescriptor... descriptors) {
    myPanel.select(descriptors);
  }
}
