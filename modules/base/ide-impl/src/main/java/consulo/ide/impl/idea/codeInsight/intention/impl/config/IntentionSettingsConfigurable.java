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

package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.language.editor.CodeInsightBundle;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

@ExtensionImpl
public class IntentionSettingsConfigurable implements ApplicationConfigurable, SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll {
  private IntentionSettingsPanel myPanel;

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (myPanel == null) {
      myPanel = new IntentionSettingsPanel();
    }
    return myPanel.getComponent();
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPanel.getIntentionTree();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified();
  }

  @Override
  public String getDisplayName() {
    return CodeInsightBundle.message("intention.settings");
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public void apply() {
    myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      myPanel.dispose();
    }
    myPanel = null;
  }

  @Override
  public Runnable enableSearch(String option) {
    return myPanel.showOption(this, option);
  }

  @Override
  @Nonnull
  public String getId() {
    return "editor.code.intentions";
  }

  public void selectIntention(String familyName) {
    myPanel.selectIntention(familyName);
  }
}
