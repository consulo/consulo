/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.execution;

import com.intellij.execution.impl.RunDialog;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.project.Project;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 05-Apr-22
 */
@Singleton
public class RunConfigurationEditorImpl implements RunConfigurationEditor {
  @Override
  public boolean editConfiguration(Project project, RunnerAndConfigurationSettings configuration, String title, @Nullable Executor executor) {
    return RunDialog.editConfiguration(project, configuration, title, executor);
  }
}
