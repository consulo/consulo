package com.intellij.tasks.timeTracking;

import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.application.util.function.Condition;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowAnchor;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.content.Content;
import consulo.project.ui.wm.content.ContentFactory;
import consulo.project.ui.wm.content.ContentManager;

/**
 * User: evgeny.zakrevsky
 * Date: 11/8/12
 */
public class TasksToolWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {

  @Override
  public boolean value(final Project project) {
    return TimeTrackingManager.getInstance(project).isTimeTrackingToolWindowAvailable();
  }

  @Override
  public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
    final ContentManager contentManager = toolWindow.getContentManager();
    final Content content = ContentFactory.SERVICE.getInstance().
      createContent(new TasksToolWindowPanel(project, toolWindow.getAnchor() == ToolWindowAnchor.LEFT ||
                                                      toolWindow.getAnchor() == ToolWindowAnchor.RIGHT), null, false);
    contentManager.addContent(content);
  }
}
