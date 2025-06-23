/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.store.internal.StorageNotificationService;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.project.ui.internal.UnknownMacroNotification;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.NotificationType;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 22-Mar-22
 */
@Singleton
@ServiceImpl
public class IdeStorageNotificationService implements StorageNotificationService {
  private static final Logger LOG = Logger.getInstance(IdeStorageNotificationService.class);

  private final Application myApplication;

  @Inject
  public IdeStorageNotificationService(Application application) {
    myApplication = application;
  }

  @Override
  public void notify(@Nonnull NotificationType notificationType, @Nonnull String title, @Nonnull String text, @Nullable ComponentManager project) {
    Notifications.SYSTEM_MESSAGES_GROUP.newOfType(consulo.project.ui.notification.NotificationType.from(notificationType))
        .title(LocalizeValue.localizeTODO(title))
        .content(LocalizeValue.localizeTODO(text))
        .notify(ObjectUtil.tryCast(project, Project.class));
  }

  @Override
  public void notifyUnknownMacros(@Nonnull TrackingPathMacroSubstitutor substitutor, @Nonnull ComponentManager project, @Nullable String componentName) {
    Set<String> unknownMacros = substitutor.getUnknownMacros(componentName);
    if (unknownMacros.isEmpty()) {
      return;
    }

    myApplication.getLastUIAccess().giveIfNeed(() -> {
      final LinkedHashSet<String> macros = new LinkedHashSet<>(unknownMacros);
      macros.removeAll(getMacrosFromExistingNotifications((Project)project));

      if (!macros.isEmpty()) {
        LOG.warn("Reporting unknown path macros " + macros + " in component " + componentName);

        String format = "<p><i>%s</i> %s undefined. <a href=\"define\">Fix it</a></p>";
        String productName = Application.get().getName().get();
        String content = String.format(format, StringUtil.join(macros, ", "), macros.size() == 1 ? "is" : "are") +
                         "<br>Path variables are used to substitute absolute paths " +
                         "in " +
                         productName +
                         " project files " +
                         "and allow project file sharing in version control systems.<br>" +
                         "Some of the files describing the current project settings contain unknown path variables " +
                         "and " +
                         productName +
                         " cannot restore those paths.";
        new UnknownMacroNotification(
            ProjectNotificationGroups.Project.newError()
                .title(LocalizeValue.localizeTODO("Load error: undefined path variables"))
                .content(LocalizeValue.localizeTODO(content))
                .optionalHyperlinkListener(
                    (notification, event) -> ProjectStorageUtil.checkUnknownMacros((ProjectEx)project, true)
                ),
            macros
        ).notify((Project)project);
      }
    });
  }


  private static List<String> getMacrosFromExistingNotifications(Project project) {
    List<String> notified = new ArrayList<>();
    NotificationsManager manager = NotificationsManager.getNotificationsManager();
    for (final UnknownMacroNotification notification : manager.getNotificationsOfType(UnknownMacroNotification.class, project)) {
      notified.addAll(notification.getMacros());
    }
    return notified;
  }
}
