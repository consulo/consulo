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
package consulo.sandboxPlugin.ide.profiler;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.execution.profiler.CallTreeBuilder;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.CollapsedProfilerDumpWriter;
import consulo.execution.profiler.model.ThreadInfo;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Random;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ActionImpl(id = "SandGenerateProfileSnapshot", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class SandGenerateProfileSnapshotAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private static final Logger LOG = Logger.getInstance(SandGenerateProfileSnapshotAction.class);

    private static final String PROCESS_NAME = "sand-sample";
    private static final long DURATION_MS = 10_000;
    private static final int SAMPLING_INTERVAL_MS = 10;
    private static final int THREAD_COUNT = 4;

    private final NotificationService myNotificationService;

    @Inject
    public SandGenerateProfileSnapshotAction(NotificationService notificationService) {
        super(
            LocalizeValue.localizeTODO("Generate Sample Profile Snapshot"),
            LocalizeValue.localizeTODO("Write a synthetic collapsed-stacks profiler snapshot into the project directory"),
            PlatformIconGroup.actionsProfilecpu()
        );
        myNotificationService = notificationService;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabled(project != null && project.getBasePath() != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }

        new Task.Backgroundable(project, LocalizeValue.localizeTODO("Generating sample profile snapshot"), true) {
            private @Nullable File mySnapshotFile;
            private @Nullable VirtualFile mySnapshot;

            @Override
            public void run(ProgressIndicator indicator) {
                CallTreeBuilder<BaseCallStackElement> builder = SandCallTreeGenerator.generate(
                    SandCallTreeGenerator.createThreads(THREAD_COUNT),
                    DURATION_MS,
                    SAMPLING_INTERVAL_MS,
                    new Random()
                );
                CollapsedProfilerDumpWriter writer = new CollapsedProfilerDumpWriter(
                    builder,
                    PROCESS_NAME,
                    System.currentTimeMillis(),
                    BaseCallStackElement::fullName,
                    ThreadInfo::getName
                );

                File file = new File(basePath, writer.getDumpFileName());
                try {
                    writer.writeDump(file, indicator);
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                mySnapshotFile = file;
                mySnapshot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                File file = mySnapshotFile;
                if (file == null) {
                    return;
                }

                Notification.Builder notification = myNotificationService.newInfo(Notifications.SYSTEM_MESSAGES_GROUP)
                    .title(LocalizeValue.localizeTODO("Sample profile snapshot created"))
                    .content(LocalizeValue.localizeTODO(file.getPath()));
                VirtualFile snapshot = mySnapshot;
                if (snapshot != null) {
                    notification.addAction(LocalizeValue.localizeTODO("Open"), () -> {
                        if (!project.isDisposed() && snapshot.isValid()) {
                            FileEditorManager.getInstance(project).openFile(snapshot, true);
                        }
                    });
                }
                notification.notify(project);
            }

            @Override
            public void onThrowable(Throwable error) {
                LOG.warn("Failed to create the sample profile snapshot", error);
                String message = error.getMessage();
                myNotificationService.newError(Notifications.SYSTEM_MESSAGES_GROUP)
                    .title(LocalizeValue.localizeTODO("Failed to create the sample profile snapshot"))
                    .content(LocalizeValue.localizeTODO(message == null ? error.getClass().getSimpleName() : message))
                    .notify(project);
            }
        }.queue();
    }
}
