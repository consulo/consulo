/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalSystem.impl.internal.service;

import consulo.application.Application;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.SyncViewManager;
import consulo.build.ui.event.BuildEventFactory;
import consulo.externalSystem.impl.internal.service.execution.ExternalSystemEventDispatcher;
import consulo.externalSystem.impl.internal.service.execution.ExternalSystemProcessHandler;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import consulo.process.ProcessOutputType;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Bridges {@link consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener}
 * callbacks to the Sync tool window via {@link SyncViewManager}.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>On {@link #onStart} a {@link DefaultBuildDescriptor} with an attached
 *       {@link ExternalSystemProcessHandler} is posted to {@link SyncViewManager} — this opens
 *       the Sync panel and wires up the stop button.</li>
 *   <li>On {@link #onTaskOutput} raw text is routed through
 *       {@link ExternalSystemEventDispatcher}, which pipes it through any registered
 *       {@link consulo.externalSystem.service.execution.ExternalSystemOutputParserProvider}
 *       parsers so that file/line errors become clickable links in the tree.</li>
 *   <li>On {@link #onSuccess} / {@link #onFailure} the process handler is terminated and the
 *       outcome is captured.</li>
 *   <li>On {@link #onEnd} the dispatcher is closed (flushing remaining buffered text through
 *       the parser chain) and a {@code FinishBuildEvent} is fired — matching the ordering used
 *       by IntelliJ IDEA.</li>
 * </ol>
 *
 * @author VISTALL
 */
public class ExternalSystemSyncViewListener extends ExternalSystemTaskNotificationListenerAdapter {

    private final Project myProject;
    private final String myProjectPath;
    private final String myProjectName;
    private final SyncViewManager mySyncViewManager;

    /** Wraps the task so the stop button in the Sync tool window can cancel it. */
    private final ExternalSystemProcessHandler myProcessHandler;

    /**
     * Routes task output through registered {@link BuildOutputParser}s and forwards all
     * build events to {@link SyncViewManager}.
     */
    private final ExternalSystemEventDispatcher myEventDispatcher;

    // Outcome captured from onSuccess / onFailure; consumed in onEnd.
    private volatile boolean mySucceeded = false;
    private volatile @Nullable Exception myException;

    /**
     * @param project          the IDE project
     * @param externalSystemId the external system id (Gradle, Maven, …)
     * @param projectPath      path to the external project file / directory
     * @param projectName      human-readable project name shown in the Sync panel title
     * @param task             the resolve task that is about to be executed
     */
    public ExternalSystemSyncViewListener(Project project,
                                          ProjectSystemId externalSystemId,
                                          String projectPath,
                                          String projectName,
                                          ExternalSystemTask task) {
        myProject = project;
        myProjectPath = projectPath;
        myProjectName = projectName;
        mySyncViewManager = SyncViewManager.getInstance(project);
        myProcessHandler = new ExternalSystemProcessHandler(task, projectName + " sync");
        // Collect BuildOutputParsers registered for this external system and wire them up.
        myEventDispatcher = new ExternalSystemEventDispatcher(task.getId(), mySyncViewManager);
    }

    // ---- ExternalSystemTaskNotificationListener -------------------------------------------------

    @Override
    public void onStart(ExternalSystemTaskId id) {
        if (myProject.isDisposed()) return;

        DefaultBuildDescriptor descriptor = new DefaultBuildDescriptor(
            id,
            myProjectName,
            myProjectPath,
            System.currentTimeMillis()
        ).withProcessHandler(myProcessHandler, null);

        // Open the Sync window automatically only on failure; stay hidden on successful silent re-imports.
        descriptor.setActivateToolWindowWhenAdded(false);
        descriptor.setActivateToolWindowWhenFailed(true);

        BuildEventFactory factory = Application.get().getInstance(BuildEventFactory.class);
        myEventDispatcher.onEvent(id, factory.createStartBuildEvent(descriptor, "Syncing..."));
    }

    @Override
    public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
        if (myProject.isDisposed()) return;
        if (text == null || text.isEmpty()) return;

        // Notify any console view attached to the process handler.
        myProcessHandler.notifyTextAvailable(text, stdOut ? ProcessOutputType.STDOUT : ProcessOutputType.STDERR);

        // Pipe through BuildOutputParser chain → structured MessageEvents (file/line errors etc.).
        try {
            myEventDispatcher.append(text);
        }
        catch (IOException ignored) {
            // Should not happen with the in-memory reader implementation.
        }
    }

    @Override
    public void onSuccess(ExternalSystemTaskId id) {
        mySucceeded = true;
        myProcessHandler.notifyProcessTerminated(0);
    }

    @Override
    public void onFailure(ExternalSystemTaskId id, Exception e) {
        mySucceeded = false;
        myException = e;
        myProcessHandler.notifyProcessTerminated(1);
    }

    @Override
    public void onEnd(ExternalSystemTaskId id) {
        if (myProject.isDisposed()) return;

        // Flush remaining buffered text through the parser chain BEFORE the finish event,
        // so all file-level error messages appear in the tree first.
        try {
            myEventDispatcher.close();
        }
        catch (IOException ignored) {
        }

        BuildEventFactory factory = Application.get().getInstance(BuildEventFactory.class);
        long eventTime = System.currentTimeMillis();

        // Fire directly to SyncViewManager (dispatcher is now closed).
        if (mySucceeded) {
            mySyncViewManager.onEvent(id, factory.createFinishBuildEvent(
                id, null, eventTime, "finished", factory.createSuccessResult()
            ));
        }
        else {
            mySyncViewManager.onEvent(id, factory.createFinishBuildEvent(
                id, null, eventTime, "failed", factory.createFailureResult(myException)
            ));
        }
    }
}
