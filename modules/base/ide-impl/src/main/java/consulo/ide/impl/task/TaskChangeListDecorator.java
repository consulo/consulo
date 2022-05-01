package consulo.ide.impl.task;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListDecorator;
import consulo.ide.impl.idea.openapi.vcs.changes.LocalChangeList;
import consulo.ide.impl.idea.tasks.LocalTask;
import consulo.ide.impl.idea.tasks.TaskManager;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;

/**
 * @author VISTALL
 * @since 2018-08-21
 */
public class TaskChangeListDecorator implements ChangeListDecorator
{
	private Project myProject;

	@Inject
	public TaskChangeListDecorator(Project project)
	{
		myProject = project;
	}

	@Override
	public void decorateChangeList(@Nonnull LocalChangeList changeList, @Nonnull ColoredTreeCellRenderer cellRenderer, boolean selected, boolean expanded, boolean hasFocus)
	{
		LocalTask task = TaskManager.getManager(myProject).getAssociatedTask(changeList);
		if(task != null && task.isIssue())
		{
			cellRenderer.setIcon(task.getIcon());
		}
	}
}
