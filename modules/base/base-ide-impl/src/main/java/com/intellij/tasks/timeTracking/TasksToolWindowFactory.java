package com.intellij.tasks.timeTracking;

import consulo.project.DumbAware;
import consulo.project.Project;
import consulo.application.util.function.Condition;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

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
