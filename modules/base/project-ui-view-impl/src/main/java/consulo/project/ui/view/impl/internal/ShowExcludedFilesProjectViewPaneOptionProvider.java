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
package consulo.project.ui.view.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.ProjectViewPaneOptionProvider;
import consulo.project.ui.view.internal.ProjectViewInternalHelper;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
@ExtensionImpl
public class ShowExcludedFilesProjectViewPaneOptionProvider extends ProjectViewPaneOptionProvider.BoolValue {
  private final class ShowExcludedFilesAction extends ToggleAction implements DumbAware {
    private ProjectViewPane myPane;

    private ShowExcludedFilesAction(ProjectViewPane pane) {
      super(ProjectUIViewLocalize.actionShowExcludedFiles(), ProjectUIViewLocalize.actionShowHideExcludedFiles());
      myPane = pane;
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return myPane.getUserData(ProjectViewInternalHelper.SHOW_EXCLUDED_FILES_KEY);
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      Boolean value = myPane.getUserData(ProjectViewInternalHelper.SHOW_EXCLUDED_FILES_KEY);
      assert value != null;
      if (value != flag) {
        myPane.putUserData(ProjectViewInternalHelper.SHOW_EXCLUDED_FILES_KEY, flag);
        myPane.updateFromRoot(true);
      }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      final ProjectView projectView = ProjectView.getInstance(e.getData(Project.KEY));
      presentation.setVisible(projectView.getCurrentProjectViewPane() == myPane);
    }
  }

  @Nonnull
  @Override
  public KeyWithDefaultValue<Boolean> getKey() {
    return ProjectViewInternalHelper.SHOW_EXCLUDED_FILES_KEY;
  }

  @Override
  public void addToolbarActions(@Nonnull ProjectViewPane pane, @Nonnull DefaultActionGroup actionGroup) {
    if (pane instanceof ProjectViewPane) {
      actionGroup.addAction(new ShowExcludedFilesAction(pane));
    }
  }
}
