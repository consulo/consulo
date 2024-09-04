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
package consulo.execution.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.project.ui.wm.ToolWindowId;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

import static consulo.project.ui.notification.NotificationDisplayType.STICKY_BALLOON;
import static consulo.project.ui.notification.NotificationDisplayType.TOOL_WINDOW;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
@ExtensionImpl
public class ExecutionNotificationGroupHolder implements NotificationGroupContributor {
    public static final NotificationGroup BASE = NotificationGroup.logOnlyGroup("Execution");
    public static final NotificationGroup EXTERNAL = new NotificationGroup("ExternalExecutableCriticalFailures",
        LocalizeValue.localizeTODO("External Executable Critical Failures"),
        STICKY_BALLOON,
        true
    );

    public static final NotificationGroup ANALYZE_THREAD_DUMP = new NotificationGroup("AnalyzeThreadDump",
        LocalizeValue.localizeTODO("Analyze thread dump"),
        TOOL_WINDOW,
        false,
        ToolWindowId.RUN
    );

    @Override
    public void contribute(@Nonnull Consumer<NotificationGroup> registrator) {
        registrator.accept(BASE);
        registrator.accept(EXTERNAL);
    }
}
