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
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * <i>Initiate</i> VFS in-memory state syncing with on-disk FS state:
 * <ul>
 * <li>marks all the VFS roots as 'dirty' (which effectively marks all VFS files 'dirty')</li>
 * <li>initiate <i>asynchronous</i> refresh, <b>without waiting for it to finish</b></li>
 * </ul>
 * Historically, this is needed for <b>quite a specific case</b>: some files belonging to project B were changed while only
 * project A is opened, and project B is opened afterward. In this case {@code fsnotifier} doesn't watch for changes in B's files
 * while project B is not opened =&gt; VFS events for changes are not generated =&gt; on B opening, there is no way to know some
 * VFS files are out of sync with the disk. {@link InitialVfsRefreshService} fixes that by forcing entire VFS in-memory state to
 * be synced -- rough, overkill, but works.
 * <p>
 * It turns out to be more generally useful, though: by some reasons, we don't have a rule to do {@code VirtualFile.isDirty() -> refresh()}
 * before any {@code VirtualFile} access -- which makes code vulnerable in cases some FS changes are not yet synced. This service
 * together with a couple others (like force sync on IDE focus lost-gain) +/- fixes that issue, by closing most of the
 * un-synced windows -- but not all of them.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class InitialVfsRefreshService implements Disposable {
    private static final Logger LOG = Logger.getInstance(InitialVfsRefreshService.class);

    public static InitialVfsRefreshService getInstance(Project project) {
        return project.getInstance(InitialVfsRefreshService.class);
    }

    private final Project myProject;

    private final AtomicBoolean myStarted = new AtomicBoolean(false);
    private final CompletableFuture<Void> myJob = new CompletableFuture<>();
    private volatile long mySessionId;

    @Inject
    public InitialVfsRefreshService(Project project) {
        myProject = project;
    }

    public void scheduleInitialVfsRefresh() {
        if (myStarted.getAndSet(true)) {
            return;
        }

        String projectId = myProject.getLocationHash();
        Application application = myProject.getApplication();
        if (Boolean.getBoolean("ij.indexes.skip.initial.refresh") || application.isUnitTestMode()) {
            LOG.debug(projectId + ": initial VFS refresh skipped");
            myJob.complete(null);
            return;
        }

        application.executeOnPooledThread(() -> {
            try {
                LOG.info(projectId + ": marking roots for initial VFS refresh");
                List<VirtualFile> roots = application.runReadAction(
                    (Supplier<List<VirtualFile>>) () -> ProjectRootManagerEx.getInstanceEx(myProject).markRootsForRefresh()
                );
                LOG.info(projectId + ": starting initial VFS refresh of " + roots.size() + " roots");
                long t = System.nanoTime();
                RefreshSession session = RefreshQueue.getInstance().createSession(true, true, () -> {
                    long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);
                    LOG.info(projectId + ": initial VFS refresh finished in " + duration + " ms");
                    myJob.complete(null);
                });
                mySessionId = session.getId();
                session.addAllFiles(roots);
                session.launch();
            }
            catch (Throwable e) {
                LOG.error(e);
                myJob.complete(null);
            }
        });
    }

    public boolean isInitialVfsRefreshFinished() {
        return myJob.isDone();
    }

    public CompletableFuture<Void> awaitInitialVfsRefreshFinished() {
        return myJob;
    }

    @Override
    public void dispose() {
        long sessionId = mySessionId;
        if (sessionId != 0) {
            RefreshQueue.getInstance().cancelSession(sessionId);
        }
    }
}
