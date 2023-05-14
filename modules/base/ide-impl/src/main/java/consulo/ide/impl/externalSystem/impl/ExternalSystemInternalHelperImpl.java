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
package consulo.ide.impl.externalSystem.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.ide.impl.idea.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.project.Project;
import consulo.ui.ex.awt.popup.AWTPopupChooserBuilder;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 04/11/2022
 */
@ServiceImpl
@Singleton
public class ExternalSystemInternalHelperImpl implements ExternalSystemInternalHelper {
  @Override
  public <T> AWTPopupChooserBuilder<T> createPopupBuilder(JTree tree) {
    return new PopupChooserBuilder<T>(tree);
  }

  @Override
  public ExternalSystemTask createExecuteSystemTask(@Nonnull ProjectSystemId externalSystemId,
                                                    @Nonnull Project project,
                                                    @Nonnull List<ExternalTaskPojo> tasksToExecute,
                                                    @Nullable String vmOptions,
                                                    @Nullable String scriptParameters,
                                                    @Nullable String debuggerSetup) {
    return new ExternalSystemExecuteTaskTask(externalSystemId, project, tasksToExecute, vmOptions, scriptParameters, debuggerSetup);
  }
}
