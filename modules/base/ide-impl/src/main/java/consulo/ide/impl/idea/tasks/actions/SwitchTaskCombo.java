/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.tasks.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ui.ex.awt.action.ComboBoxButtonImpl;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.TaskSettings;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopup;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class SwitchTaskCombo extends ComboBoxAction implements DumbAware {
  @Nonnull
  public JComponent createCustomComponent(final Presentation presentation, String place) {
    return new ComboBoxButtonImpl(this, presentation);
  }

  @Nonnull
  @Override
  public JBPopup createPopup(@Nonnull JComponent component, @Nonnull DataContext context, @Nonnull Runnable onDispose) {
    return SwitchTaskAction.createPopup(context, onDispose, false);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getData(Project.KEY);
    ComboBoxButtonImpl button = (ComboBoxButtonImpl)presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY);
    if (project == null || project.isDefault() || project.isDisposed() || button == null) {
      presentation.setEnabled(false);
      presentation.setText("");
      presentation.setIcon(null);
    }
    else {
      TaskManager taskManager = TaskManager.getManager(project);
      LocalTask activeTask = taskManager.getActiveTask();
      presentation.setVisible(true);
      presentation.setEnabled(true);

      if (isImplicit(activeTask) && taskManager.getAllRepositories().length == 0 && !TaskSettings.getInstance().ALWAYS_DISPLAY_COMBO) {
        presentation.setVisible(false);
      }
      else {
        String s = getText(activeTask);
        presentation.setText(s);
        presentation.setIcon(activeTask.getIcon());
        presentation.setDescription(activeTask.getSummary());
      }
    }
  }

  @Nonnull
  @Override
  protected DefaultActionGroup createPopupActionGroup(JComponent button) {
    throw new IllegalArgumentException();
  }

  private static boolean isImplicit(LocalTask activeTask) {
    return activeTask.isDefault() && Comparing.equal(activeTask.getCreated(), activeTask.getUpdated());
  }

  private static String getText(LocalTask activeTask) {
    String text = activeTask.getPresentableName();
    return StringUtil.first(text, 50, true);
  }
}