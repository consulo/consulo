/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.ui;

import consulo.application.dumb.DumbAware;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.ui.awt.ExternalSystemTasksPanel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

import java.util.Locale;

/**
 * @author Denis Zhdanov
 * @since 5/13/13 4:15 PM
 */
public abstract class AbstractExternalSystemToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final NotificationGroup myNotificationGroup;

  protected AbstractExternalSystemToolWindowFactory(@Nonnull ProjectSystemId id) {
    myExternalSystemId = id;
    myNotificationGroup = NotificationGroup.toolWindowGroup("notification.group.id." + id.toString().toLowerCase(Locale.ROOT),
                                                            myExternalSystemId.getDisplayName(),
                                                            myExternalSystemId.getToolWindowId(),
                                                            true);
  }

  @Nonnull
  @Override
  public final String getId() {
    return myExternalSystemId.getToolWindowId();
  }

  @Nonnull
  @Override
  public final LocalizeValue getDisplayName() {
    return myExternalSystemId.getDisplayName();
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull final Project project, final ToolWindow toolWindow) {
    ContentManager contentManager = toolWindow.getContentManager();
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    assert manager != null;
    ExternalSystemTasksPanel panel = new ExternalSystemTasksPanel(project, myExternalSystemId, myNotificationGroup);
    Content tasksContent = ContentFactory.getInstance().createContent(panel, "", true);
    contentManager.addContent(tasksContent);
  }

  @Override
  public boolean validate(@Nonnull Project project) {
    if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == Boolean.TRUE) {
      return true;
    }

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    if (manager == null) {
      return false;
    }

    AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(project);
    return settings != null && !settings.getLinkedProjectsSettings().isEmpty();
  }
}
