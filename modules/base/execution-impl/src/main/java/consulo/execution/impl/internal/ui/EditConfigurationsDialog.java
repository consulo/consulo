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

package consulo.execution.impl.internal.ui;

import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.project.Project;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Splitter;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class EditConfigurationsDialog extends WholeWestSingleConfigurableEditor implements RunConfigurable.RunDialogBase {
  protected Executor myExecutor;

  public EditConfigurationsDialog(final Project project) {
    super(project, new RunConfigurable(project), "consulo.execution.impl.internal.ui.EditConfigurationsDialog");
    getConfigurable().setRunDialog(this);
    setTitle(ExecutionLocalize.runDebugDialogTitle());
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
  public Size2D getDefaultSize() {
    return new Size2D(750, 500);
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