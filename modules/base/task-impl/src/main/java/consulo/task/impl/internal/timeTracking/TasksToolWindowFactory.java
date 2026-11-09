package consulo.task.impl.internal.timeTracking;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowId;
import consulo.task.icon.TaskIconGroup;
import consulo.task.impl.internal.TasksToolWindowState;
import consulo.task.impl.internal.issue.IssueTabsSync;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * @author evgeny.zakrevsky
 * @since 2012-11-08
 */
@ExtensionImpl
public class TasksToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    @Override
    public String getId() {
        return ToolWindowId.TASKS;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        LocalTasksPanel panel = new LocalTasksPanel(
            project,
            toolWindow.getAnchor() == ToolWindowAnchor.LEFT || toolWindow.getAnchor() == ToolWindowAnchor.RIGHT
        );
        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), TaskLocalize.toolwindowLocalTasks().get(), false);
        content.setDisposer(panel);
        contentManager.addContent(content);
        new IssueTabsSync(project, toolWindow).install(contentManager);
        TasksToolWindowState.update(project);
    }

    
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    
    @Override
    public Image getIcon() {
        return TaskIconGroup.clock();
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return TaskLocalize.toolwindowIssuesTitle();
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return TasksToolWindowState.isAvailable(project);
    }
}
