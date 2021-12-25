/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import consulo.execution.ui.CommonProgramParametersLayout;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.dialog.DialogService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class SandConfiguration extends RunConfigurationBase {
  public SandConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  @Nonnull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new SettingsEditor<RunConfiguration>() {
      @Override
      protected void resetEditorFrom(RunConfiguration s) {

      }

      @Override
      protected void applyEditorTo(RunConfiguration s) throws ConfigurationException {

      }

      @RequiredUIAccess
      @Nonnull
      @Override
      protected Component createUIComponent() {
        DialogService dialogService = Application.get().getInstance(DialogService.class);
        CommonProgramParametersLayout layout = new CommonProgramParametersLayout(dialogService);
        layout.build();
        return layout.getComponent();
      }
    };
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {

  }

  @Nullable
  @Override
  public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment env) throws ExecutionException {
    return null;
  }
}
