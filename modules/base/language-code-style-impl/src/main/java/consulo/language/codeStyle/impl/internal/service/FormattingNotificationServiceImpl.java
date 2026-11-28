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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.service.FormattingNotificationService;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.ex.action.AnAction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class FormattingNotificationServiceImpl implements FormattingNotificationService {
    private final Project myProject;

    @Inject
    public FormattingNotificationServiceImpl(Project project) {
        myProject = project;
    }

    @Override
    public void reportError(
        NotificationGroup group,
        @Nullable String displayId,
        LocalizeValue title,
        LocalizeValue message,
        AnAction... actions
    ) {
        logIfHeadlessOrRun(title, message, () -> FormattingNotificationUtil.reportError(myProject, group, displayId, title, message, actions));
    }

    @Override
    public void reportErrorAndNavigate(
        NotificationGroup group,
        @Nullable String displayId,
        LocalizeValue title,
        LocalizeValue message,
        FormattingContext context,
        int offset
    ) {
        logIfHeadlessOrRun(
            title,
            message,
            () -> FormattingNotificationUtil.reportErrorAndNavigate(myProject, group, displayId, title, message, context, offset)
        );
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void logIfHeadlessOrRun(LocalizeValue title, LocalizeValue message, Runnable block) {
        if (Application.get().isHeadlessEnvironment()) {
            System.err.println(title.get() + ": " + message.get());
        }
        else {
            block.run();
        }
    }
}
