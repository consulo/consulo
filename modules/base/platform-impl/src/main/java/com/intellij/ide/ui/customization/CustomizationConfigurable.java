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
package com.intellij.ide.ui.customization;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * User: anna
 * Date: Mar 17, 2005
 */
public class CustomizationConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private CustomizableActionsPanel myPanel;

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new CustomizableActionsPanel();
    }
    return myPanel.getPanel();
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.customizations");
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if(myPanel != null) {
      Disposer.dispose(myPanel);
      myPanel = null;
    }
  }

  @Override
  @Nonnull
  public String getId() {
    return "preferences.customizations";
  }
}
