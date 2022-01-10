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

package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.openapi.options.ex.WholeWestSingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Couple;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class EditConfigurationsDialog extends WholeWestSingleConfigurableEditor implements RunConfigurable.RunDialogBase {
  protected Executor myExecutor;

  public EditConfigurationsDialog(final Project project) {
    super(project, new RunConfigurable(project), "com.intellij.execution.impl.EditConfigurationsDialog");
    getConfigurable().setRunDialog(this);
    setTitle(ExecutionBundle.message("run.debug.dialog.title"));
  }

  @Override
  public RunConfigurable getConfigurable() {
    return (RunConfigurable)super.getConfigurable();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
    RunConfigurable configurable = getConfigurable();
    configurable.createComponent(getDisposable());
    configurable.setWholePanel(rootPanel);
    Splitter splitter = configurable.getSplitter();
    return Couple.of(splitter.getFirstComponent(), splitter.getSecondComponent());
  }

  @Override
  protected void doOKAction() {
    RunConfigurable configurable = getConfigurable();
    super.doOKAction();
    if (isOK()) {
      // if configurable was not modified, apply was not called and Run Configurable has not called 'updateActiveConfigurationFromSelected'
      configurable.updateActiveConfigurationFromSelected();
    }
  }

  @Override
  public Dimension getDefaultSize() {
    return new Dimension(750, 500);
  }

  @Nullable
  @Override
  public Executor getExecutor() {
    return myExecutor;
  }

  @Override
  public void setOKActionEnabled(boolean isEnabled) {
    super.setOKActionEnabled(isEnabled);
  }
}