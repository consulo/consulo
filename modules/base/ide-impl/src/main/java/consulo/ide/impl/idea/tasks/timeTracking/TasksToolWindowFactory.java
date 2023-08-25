package consulo.ide.impl.idea.tasks.timeTracking;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowId;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * User: evgeny.zakrevsky
 * Date: 11/8/12
 */
@ExtensionImpl
public class TasksToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Nonnull
  @Override
  public String getId() {
    return ToolWindowId.TASKS;
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
    final ContentManager contentManager = toolWindow.getContentManager();
    final Content content = ContentFactory.getInstance().
            createContent(new TasksToolWindowPanel(project, toolWindow.getAnchor() == ToolWindowAnchor.LEFT || toolWindow.getAnchor() == ToolWindowAnchor.RIGHT), null, false);
    contentManager.addContent(content);
  }

  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.RIGHT;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return TaskIconGroup.clock();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Time Tracking");
  }

  @Override
  public boolean shouldBeAvailable(@Nonnull Project project) {
    return TimeTrackingManager.getInstance(project).isTimeTrackingToolWindowAvailable();
  }
}
