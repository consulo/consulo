// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.codeStyle.impl.internal.service;

import consulo.application.Application;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

final class FormattingNotificationUtil {
    private FormattingNotificationUtil() {
    }

    static void reportErrorAndNavigate(
        Project project,
        NotificationGroup group,
        @Nullable String displayId,
        LocalizeValue title,
        LocalizeValue message,
        FormattingContext context,
        int offset
    ) {
        VirtualFile virtualFile = context.getVirtualFile();
        if (virtualFile == null) {
            return;
        }
        Application.get().invokeLater(() -> {
            FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(virtualFile);
            if (editors.length > 0) {
                reportError(project, group, displayId, title, message);
                for (FileEditor editor : editors) {
                    if (editor instanceof TextEditor) {
                        navigateToFile(project, virtualFile, offset);
                        break;
                    }
                }
            }
            else {
                Notification.Builder builder = newError(group, displayId, title, message);
                builder.addAction(
                    CodeStyleLocalize.formattingServiceOpenFile(virtualFile.getName()),
                    () -> navigateToFile(project, virtualFile, offset)
                );
                builder.notify(project);
            }
        });
    }

    static void reportError(
        Project project,
        NotificationGroup group,
        @Nullable String displayId,
        LocalizeValue title,
        LocalizeValue message,
        AnAction... actions
    ) {
        Notification.Builder builder = newError(group, displayId, title, message);
        for (AnAction action : actions) {
            builder.addAction(action);
        }
        builder.notify(project);
    }

    private static Notification.Builder newError(
        NotificationGroup group,
        @Nullable String displayId,
        LocalizeValue title,
        LocalizeValue message
    ) {
        Notification.Builder builder = NotificationService.getInstance().newError(group).title(title).content(message);
        if (displayId != null) {
            builder.displayId(displayId);
        }
        return builder;
    }

    private static void navigateToFile(Project project, VirtualFile file, int offset) {
        OpenFileDescriptorFactory.getInstance(project).newBuilder(file).offset(offset).build().navigate(true);
    }
}
