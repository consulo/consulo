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

/*
 * User: anna
 * Date: 28-Jun-2007
 */
package consulo.ide.impl.idea.internal;

import consulo.execution.RunManager;
import consulo.execution.configuration.ConfigurationType;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;

import java.util.List;

public class DumpConfigurationTypesAction extends AnAction implements DumbAware {
  public DumpConfigurationTypesAction() {
    super("Dump Configurations");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    final List<ConfigurationType> factories = RunManager.getInstance(project).getConfigurationFactories();
    for (ConfigurationType factory : factories) {
      System.out.println(factory.getDisplayName() + " : " + factory.getId());
    }
  }

  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}