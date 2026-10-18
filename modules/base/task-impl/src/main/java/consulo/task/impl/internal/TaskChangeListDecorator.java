package consulo.task.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.change.ChangeListDecorator;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.project.Project;
import consulo.ui.TextItemPresentation;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-08-21
 */
@ExtensionImpl
public class TaskChangeListDecorator implements ChangeListDecorator {
  private Project myProject;

  @Inject
  public TaskChangeListDecorator(Project project) {
    myProject = project;
  }

  @Override
  public void decorateChangeList(LocalChangeList changeList, TextItemPresentation presentation, boolean selected, boolean expanded, boolean hasFocus) {
    LocalTask task = TaskManager.getManager(myProject).getAssociatedTask(changeList);
    if (task != null && task.isIssue()) {
      presentation.withIcon(task.getIcon());
    }
  }
}
