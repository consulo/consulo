package consulo.tasks.impl;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListDecorator;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskManager;
import com.intellij.ui.ColoredTreeCellRenderer;

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
