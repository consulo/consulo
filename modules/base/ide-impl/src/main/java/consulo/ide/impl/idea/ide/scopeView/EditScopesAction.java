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

package consulo.ide.impl.idea.ide.scopeView;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.MasterDetailsStateService;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2006-01-27
 */
public class EditScopesAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(EditScopesAction.class);

  public EditScopesAction() {
    getTemplatePresentation().setIcon(AllIcons.Ide.LocalScope);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    final String scopeName = ProjectView.getInstance(project).getCurrentProjectViewPane().getSubId();
    LOG.assertTrue(scopeName != null);
    final ScopeChooserConfigurable scopeChooserConfigurable = new ScopeChooserConfigurable(project, () -> MasterDetailsStateService.getInstance(project));
    ShowSettingsUtil.getInstance()
      .editConfigurable(project, scopeChooserConfigurable, () -> scopeChooserConfigurable.selectNodeInTree(scopeName));
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(false);
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    if (project != null) {
      final ProjectViewPane projectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
      if (projectViewPane != null) {
        final String scopeName = projectViewPane.getSubId();
        if (scopeName != null) {
          e.getPresentation().setEnabled(true);
        }
      }
    }
  }
}
