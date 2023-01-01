package consulo.ide.impl.task;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListDecorator;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

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
  public void decorateChangeList(@Nonnull LocalChangeList changeList, @Nonnull ColoredTreeCellRenderer cellRenderer, boolean selected, boolean expanded, boolean hasFocus) {
    LocalTask task = TaskManager.getManager(myProject).getAssociatedTask(changeList);
    if (task != null && task.isIssue()) {
      cellRenderer.setIcon(task.getIcon());
    }
  }
}
