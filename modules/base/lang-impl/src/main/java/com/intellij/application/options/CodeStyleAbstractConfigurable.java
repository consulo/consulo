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
package com.intellij.application.options;

import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import consulo.disposer.Disposer;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.Set;

public abstract class CodeStyleAbstractConfigurable implements Configurable, OptionsContainingConfigurable {
  private CodeStyleAbstractPanel myPanel;
  private final CodeStyleSettings mySettings;
  private final CodeStyleSettings myCloneSettings;
  private final String myDisplayName;

  public CodeStyleAbstractConfigurable(@Nonnull CodeStyleSettings settings, CodeStyleSettings cloneSettings,
                                       final String displayName) {
    mySettings = settings;
    myCloneSettings = cloneSettings;
    myDisplayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    myPanel = createPanel(myCloneSettings);
    return myPanel.getPanel();
  }

  protected abstract CodeStyleAbstractPanel createPanel(final CodeStyleSettings settings);

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myPanel != null) {
      myPanel.apply(mySettings);
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    reset(mySettings);
  }

  public void resetFromClone(){
    reset(myCloneSettings);
  }

  public void reset(CodeStyleSettings settings) {
    if (myPanel != null) {
      myPanel.reset(settings);
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified(mySettings);
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      Disposer.dispose(myPanel);
      myPanel = null;
    }
  }

  public CodeStyleAbstractPanel getPanel() {
    return myPanel;
  }

  public void setModel(@Nonnull CodeStyleSchemesModel model) {
    if (myPanel != null) {
      myPanel.setModel(model);
    }
  }

  public void onSomethingChanged() {
    myPanel.onSomethingChanged();
  }

  @Override
  public Set<String> processListOptions() {
    return myPanel.processListOptions();
  }

  protected CodeStyleSettings getCurrentSettings() {
    return mySettings;
  }
}
