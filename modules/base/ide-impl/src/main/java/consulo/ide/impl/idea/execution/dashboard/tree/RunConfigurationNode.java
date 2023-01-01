/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.dashboard.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.executor.Executor;
import consulo.execution.RunManager;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.ide.impl.idea.execution.dashboard.DashboardRunConfigurationNode;
import consulo.ide.impl.idea.execution.dashboard.DashboardRunConfigurationStatus;
import consulo.ide.impl.idea.execution.dashboard.RunDashboardContributor;
import consulo.execution.ui.RunContentDescriptor;
import consulo.ide.impl.idea.execution.ui.RunContentManagerImpl;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.content.Content;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * @author konstantin.aleev
 */
class RunConfigurationNode  extends AbstractTreeNode<Pair<RunnerAndConfigurationSettings, RunContentDescriptor>>
        implements DashboardRunConfigurationNode {
  public RunConfigurationNode(Project project, @Nonnull Pair<RunnerAndConfigurationSettings, RunContentDescriptor> value) {
    super(project, value);
  }

  @Override
  @Nonnull
  public RunnerAndConfigurationSettings getConfigurationSettings() {
    //noinspection ConstantConditions ???
    return getValue().getFirst();
  }

  @Nullable
  @Override
  public RunContentDescriptor getDescriptor() {
    //noinspection ConstantConditions ???
    return getValue().getSecond();
  }

  @Override
  protected void update(PresentationData presentation) {
    RunnerAndConfigurationSettings configurationSettings = getConfigurationSettings();
    boolean isStored = RunManager.getInstance(getProject()).hasSettings(configurationSettings);
    presentation.addText(configurationSettings.getName(),
                         isStored ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
    RunDashboardContributor contributor = RunDashboardContributor.getContributor(configurationSettings.getType());
    Image icon = null;
    if (contributor != null) {
      DashboardRunConfigurationStatus status = contributor.getStatus(this);
      if (DashboardRunConfigurationStatus.STARTED.equals(status)) {
        icon = getExecutorIcon();
      }
      else if (DashboardRunConfigurationStatus.FAILED.equals(status)) {
        icon = status.getIcon();
      }
    }
    if (icon == null) {
      icon = RunManagerEx.getInstanceEx(getProject()).getConfigurationIcon(configurationSettings);
    }
    presentation.setIcon(isStored ? icon : ImageEffects.grayed(icon));

    if (contributor != null) {
      contributor.updatePresentation(presentation, this);
    }
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return Collections.emptyList();
  }

  @Nullable
  private Image getExecutorIcon() {
    Content content = getContent();
    if (content != null) {
      if (!RunContentManagerImpl.isTerminated(content)) {
        Executor executor = RunContentManagerImpl.getExecutorByContent(content);
        if (executor != null) {
          return executor.getIcon();
        }
      }
    }
    return null;
  }
}
