/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.browsers;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.ide.IdeBundle;
import consulo.ide.ServiceManager;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.webBrowser.WebSearchOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.webBrowser.WebBrowser;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

@ExtensionImpl
public class BrowserSettings implements ApplicationConfigurable, SearchableConfigurable, Configurable.NoScroll {
  private BrowserSettingsPanel myPanel;

  private Provider<WebSearchOptions> myWebSearchOptionsProvider;

  @Deprecated
  @DeprecationInfo("Don't use custom creating. Use initialize via extensions")
  public BrowserSettings() {
    this(() -> ServiceManager.getService(WebSearchOptions.class));
  }

  @Inject
  public BrowserSettings(Provider<WebSearchOptions> webSearchOptionsProvider) {
    myWebSearchOptionsProvider = webSearchOptionsProvider;
  }

  @Override
  @Nonnull
  public String getId() {
    return "web.browsers";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return IdeBundle.message("browsers.settings");
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (myPanel == null) {
      myPanel = new BrowserSettingsPanel(myWebSearchOptionsProvider, uiDisposable);
    }
    return myPanel.getComponent();
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

  public void selectBrowser(@Nonnull WebBrowser browser) {
    if(myPanel == null) {
      throw new IllegalArgumentException("not initialized ui");
    }
    myPanel.selectBrowser(browser);
  }
}
