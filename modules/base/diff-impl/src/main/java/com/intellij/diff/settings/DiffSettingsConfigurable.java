/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

public class DiffSettingsConfigurable implements SearchableConfigurable {
  private DiffSettingsPanel mySettingsPane;

  @Nonnull
  @Override
  public String getId() {
    return getHelpTopic();
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Diff";
  }

  @Nonnull
  @Override
  public String getHelpTopic() {
    return "diff.base";
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent() {
    if (mySettingsPane == null) {
      mySettingsPane = new DiffSettingsPanel();
    }
    return mySettingsPane.getPanel();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return mySettingsPane != null && mySettingsPane.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (mySettingsPane != null) {
      mySettingsPane.apply();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (mySettingsPane != null) {
      mySettingsPane.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    mySettingsPane = null;
  }
}
