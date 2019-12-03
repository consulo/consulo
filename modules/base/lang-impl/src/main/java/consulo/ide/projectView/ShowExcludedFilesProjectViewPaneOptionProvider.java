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
package consulo.ide.projectView;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.ide.projectView.impl.ProjectViewPaneOptionProvider;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
public class ShowExcludedFilesProjectViewPaneOptionProvider extends ProjectViewPaneOptionProvider.BoolValue {
  public static final KeyWithDefaultValue<Boolean> KEY = KeyWithDefaultValue.create("show-excluded-files", Boolean.FALSE);

  private final class ShowExcludedFilesAction extends ToggleAction implements DumbAware {
    private AbstractProjectViewPane myPane;

    private ShowExcludedFilesAction(AbstractProjectViewPane pane) {
      super(IdeBundle.message("action.show.excluded.files"), IdeBundle.message("action.show.hide.excluded.files"), null);
      myPane = pane;
    }

    @Override
    public boolean isSelected(AnActionEvent event) {
      return myPane.getUserData(KEY);
    }

    @Override
    public void setSelected(AnActionEvent event, boolean flag) {
      Boolean value = myPane.getUserData(KEY);
      assert value != null;
      if (value != flag) {
        myPane.putUserData(KEY, flag);
        myPane.updateFromRoot(true);
      }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      final ProjectView projectView = ProjectView.getInstance(e.getProject());
      presentation.setVisible(projectView.getCurrentProjectViewPane() == myPane);
    }
  }

  @Nonnull
  @Override
  public KeyWithDefaultValue<Boolean> getKey() {
    return KEY;
  }

  @Override
  public void addToolbarActions(@Nonnull AbstractProjectViewPane pane, @Nonnull DefaultActionGroup actionGroup) {
    if (pane instanceof ProjectViewPane) {
      actionGroup.addAction(new ShowExcludedFilesAction(pane)).setAsSecondary(true);
    }
  }
}
