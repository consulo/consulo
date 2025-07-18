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
package consulo.project.impl.internal;

import consulo.application.Application;
import consulo.application.macro.PathMacros;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.project.internal.ProjectEx;
import consulo.project.ui.internal.UnknownMacroNotification;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.*;

public class ProjectStorageUtil {
    public static class UnableToSaveProjectNotification extends Notification {
        private Project myProject;
        private final List<String> myFileNames;

        private UnableToSaveProjectNotification(@Nonnull Project project, Collection<File> readOnlyFiles) {
            super(
                NotificationService.getInstance()
                    .newError(ProjectNotificationGroups.Project)
                    .title(LocalizeValue.localizeTODO("Could not save project!"))
                    .content(LocalizeValue.localizeTODO(buildMessage()))
                    .hyperlinkListener((notification, event) -> {
                        UnableToSaveProjectNotification unableToSaveProjectNotification = (UnableToSaveProjectNotification) notification;
                        Project _project = unableToSaveProjectNotification.getProject();
                        notification.expire();

                        if (_project != null && !_project.isDisposed()) {
                            _project.save();
                        }
                    })
            );

            myProject = project;
            myFileNames = ContainerUtil.map(readOnlyFiles, File::getPath);
        }

        public List<String> getFileNames() {
            return myFileNames;
        }

        private static String buildMessage() {
            return "<p>Unable to save project files. Please ensure project files are writable and you have permissions to modify them. " +
                "<a href=\"\">Try to save project again</a>.</p>";
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
    public static void checkUnknownMacros(ProjectEx project, boolean showDialog) {
        IProjectStore stateStore = project.getInstance(IProjectStore.class);

        TrackingPathMacroSubstitutor[] substitutors = stateStore.getSubstitutors();
        Set<String> unknownMacros = new HashSet<>();
        for (TrackingPathMacroSubstitutor substitutor : substitutors) {
            unknownMacros.addAll(substitutor.getUnknownMacros(null));
        }

        if (!unknownMacros.isEmpty()) {
            if (!showDialog || project.getApplication()
                .getInstance(ProjectCheckMacroService.class)
                .checkMacros(project, new HashSet<>(unknownMacros))) {
                PathMacros pathMacros = PathMacros.getInstance();
                Set<String> macros2invalidate = new HashSet<>(unknownMacros);
                for (Iterator it = macros2invalidate.iterator(); it.hasNext(); ) {
                    String macro = (String) it.next();
                    String value = pathMacros.getValue(macro);
                    if ((value == null || value.trim().isEmpty()) && !pathMacros.isIgnoredMacroName(macro)) {
                        it.remove();
                    }
                }

                if (!macros2invalidate.isEmpty()) {
                    Set<String> components = new HashSet<>();
                    for (TrackingPathMacroSubstitutor substitutor : substitutors) {
                        components.addAll(substitutor.getComponents(macros2invalidate));
                    }

                    for (TrackingPathMacroSubstitutor substitutor : substitutors) {
                        substitutor.invalidateUnknownMacros(macros2invalidate);
                    }

                    UnknownMacroNotification[] notifications =
                        NotificationsManager.getNotificationsManager().getNotificationsOfType(UnknownMacroNotification.class, project);
                    for (UnknownMacroNotification notification : notifications) {
                        if (macros2invalidate.containsAll(notification.getMacros())) {
                            notification.expire();
                        }
                    }

                    Application.get().runWriteAction(() -> stateStore.reinitComponents(components, true));
                }
            }
        }
    }

    @Nonnull
    public static String getStoreDir(@Nonnull Project project) {
        return project.getBasePath() + "/" + Project.DIRECTORY_STORE_FOLDER;
    }

    public static void dropUnableToSaveProjectNotification(@Nonnull Project project, Collection<File> readOnlyFiles) {
        UnableToSaveProjectNotification[] notifications =
            NotificationsManager.getNotificationsManager().getNotificationsOfType(UnableToSaveProjectNotification.class, project);
        if (notifications.length == 0) {
            Notifications.Bus.notify(new UnableToSaveProjectNotification(project, readOnlyFiles), project);
        }
    }
}
