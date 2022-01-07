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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.options.SimpleConfigurable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

public class CodeCompletionOptions extends SimpleConfigurable<CodeCompletionPanel> implements SearchableConfigurable {

  @RequiredUIAccess
  @Nonnull
  @Override
  protected CodeCompletionPanel createPanel(Disposable uiDisposable) {
    return new CodeCompletionPanel(ActionManager.getInstance());
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull CodeCompletionPanel component) {
    return component.isModified();
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull CodeCompletionPanel component) throws ConfigurationException {
    component.apply();
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull CodeCompletionPanel component) {
    component.reset();
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.code.completion");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.completion";
  }

  @Override
  @Nonnull
  public String getId() {
    return "editor.preferences.completion";
  }
}
