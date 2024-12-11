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
package consulo.webBrowser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.webBrowser.WebBrowser;
import consulo.webBrowser.WebSearchOptions;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;

@ExtensionImpl
public class BrowserSettings implements ApplicationConfigurable, SearchableConfigurable, Configurable.NoScroll {
  private BrowserSettingsPanel myPanel;

  private Provider<WebSearchOptions> myWebSearchOptionsProvider;

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

  @Nonnull
  @Override
  public String getDisplayName() {
    return WebBrowserLocalize.browsersSettings().get();
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
