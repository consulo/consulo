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
package consulo.ide.impl.idea.internal;

import consulo.application.dumb.DumbAware;
import consulo.execution.RunManager;
import consulo.execution.configuration.ConfigurationType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author anna
 * @since 2007-06-28
 */
public class DumpConfigurationTypesAction extends AnAction implements DumbAware {
  public DumpConfigurationTypesAction() {
    super("Dump Configurations");
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(Project.KEY);
    final List<ConfigurationType> factories = RunManager.getInstance(project).getConfigurationFactories();
    for (ConfigurationType factory : factories) {
      System.out.println(factory.getDisplayName() + " : " + factory.getId());
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(e.hasData(Project.KEY));
  }
}