/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class IntentionSettingsConfigurable implements SearchableConfigurable, Configurable.HoldPreferredFocusedComponent {
  @NonNls
  public static final String HELP_ID = "preferences.intentionPowerPack";
  private IntentionSettingsPanel myPanel;

  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new IntentionSettingsPanel();
    }
    return myPanel.getComponent();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPanel.getIntentionTree();
  }

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public String getDisplayName() {
    return CodeInsightBundle.message("intention.settings");
  }

  @Override
  public void reset() {
    myPanel.reset();
  }

  @Override
  public void apply() {
    myPanel.apply();
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      myPanel.dispose();
    }
    myPanel = null;
  }

  @Override
  public String getHelpTopic() {
    return HELP_ID;
  }

  @Override
  public Runnable enableSearch(String option) {
    return myPanel.showOption(this, option);
  }

  @Override
  @NotNull
  public String getId() {
    return HELP_ID;
  }

  public void selectIntention(String familyName) {
    myPanel.selectIntention(familyName);
  }
}
