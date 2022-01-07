/*
 * Copyright 2013-2020 consulo.io
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
package consulo.components.impl.stores;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.impl.ProjectMacrosUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.*;

public class ProjectStorageUtil {

  public static class UnableToSaveProjectNotification extends Notification {
    private Project myProject;
    private final List<String> myFileNames;

    private UnableToSaveProjectNotification(@Nonnull final Project project, final Collection<File> readOnlyFiles) {
      super("Project Settings", "Could not save project!", buildMessage(), NotificationType.ERROR, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
          final UnableToSaveProjectNotification unableToSaveProjectNotification = (UnableToSaveProjectNotification)notification;
          final Project _project = unableToSaveProjectNotification.getProject();
          notification.expire();

          if (_project != null && !_project.isDisposed()) {
            _project.save();
          }
        }
      });

      myProject = project;
      myFileNames = ContainerUtil.map(readOnlyFiles, File::getPath);
    }

    public List<String> getFileNames() {
      return myFileNames;
    }

    private static String buildMessage() {
      final StringBuilder sb = new StringBuilder("<p>Unable to save project files. Please ensure project files are writable and you have permissions to modify them.");
      return sb.append(" <a href=\"\">Try to save project again</a>.</p>").toString();
    }

    public Project getProject() {
      return myProject;
    }

    @Override
    public void expire() {
      myProject = null;
      super.expire();
    }
  }

  @RequiredUIAccess
  public static void checkUnknownMacros(ProjectEx project, final boolean showDialog) {
    final IProjectStore stateStore = project.getStateStore();

    final TrackingPathMacroSubstitutor[] substitutors = stateStore.getSubstitutors();
    final Set<String> unknownMacros = new HashSet<>();
    for (final TrackingPathMacroSubstitutor substitutor : substitutors) {
      unknownMacros.addAll(substitutor.getUnknownMacros(null));
    }

    if (!unknownMacros.isEmpty()) {
      if (!showDialog || ProjectMacrosUtil.checkMacros(project, new HashSet<>(unknownMacros))) {
        final PathMacros pathMacros = PathMacros.getInstance();
        final Set<String> macros2invalidate = new HashSet<>(unknownMacros);
        for (Iterator it = macros2invalidate.iterator(); it.hasNext(); ) {
          final String macro = (String)it.next();
          final String value = pathMacros.getValue(macro);
          if ((value == null || value.trim().isEmpty()) && !pathMacros.isIgnoredMacroName(macro)) {
            it.remove();
          }
        }

        if (!macros2invalidate.isEmpty()) {
          final Set<String> components = new HashSet<>();
          for (TrackingPathMacroSubstitutor substitutor : substitutors) {
            components.addAll(substitutor.getComponents(macros2invalidate));
          }

          for (final TrackingPathMacroSubstitutor substitutor : substitutors) {
            substitutor.invalidateUnknownMacros(macros2invalidate);
          }

          final UnknownMacroNotification[] notifications = NotificationsManager.getNotificationsManager().getNotificationsOfType(UnknownMacroNotification.class, project);
          for (final UnknownMacroNotification notification : notifications) {
            if (macros2invalidate.containsAll(notification.getMacros())) notification.expire();
          }

          ApplicationManager.getApplication().runWriteAction(() -> stateStore.reinitComponents(components, true));
        }
      }
    }
  }

  public static void dropUnableToSaveProjectNotification(@Nonnull final Project project, Collection<File> readOnlyFiles) {
    final UnableToSaveProjectNotification[] notifications = NotificationsManager.getNotificationsManager().getNotificationsOfType(UnableToSaveProjectNotification.class, project);
    if (notifications.length == 0) {
      Notifications.Bus.notify(new UnableToSaveProjectNotification(project, readOnlyFiles), project);
    }
  }
}
