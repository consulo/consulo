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
package consulo.it;

import consulo.application.Application;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.impl.internal.DumbServiceImpl;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * The projects of one test, injected by {@link HeadlessProjectExtension}. A test opens as many projects as it needs over
 * directories it prepared itself, or over fresh temporary directories, and every project still open when the test ends
 * is settled and closed by the extension, so no scanning or dumb task of one test survives into the next.
 *
 * @author VISTALL
 */
public final class HeadlessProjects {
    public static final long TIMEOUT_SECONDS = 60;

    private final String myName;
    private final Application myApplication;
    private final List<Project> myProjects = new CopyOnWriteArrayList<>();
    private volatile @Nullable Project myDefaultProject;

    HeadlessProjects(String name, Application application) {
        myName = name;
        myApplication = application;
    }

    /**
     * A fresh temporary directory named after the test, for a test which lays out files before opening a project over them.
     */
    public Path newDirectory() throws IOException {
        return Files.createTempDirectory("consulo-it-" + myName);
    }

    /**
     * Opens a project over a fresh temporary directory.
     */
    public Project open() throws Exception {
        return open(newDirectory());
    }

    /**
     * Opens a project over the given directory the way the IDE does; the project is closed when the test ends unless the test
     * closed it already.
     */
    public Project open(Path directory) throws Exception {
        ProjectManager projectManager = myApplication.getInstance(ProjectManager.class);
        Project project = projectManager
            .openProjectAsync(directory, myApplication.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (project == null) {
            throw new AssertionError("the project over " + directory + " did not open");
        }
        myProjects.add(project);
        return project;
    }

    /**
     * The project a test gets when it asks for a bare {@link Project} parameter: opened over a fresh directory on first use.
     */
    public Project defaultProject() throws Exception {
        Project project = myDefaultProject;
        if (project == null) {
            synchronized (this) {
                project = myDefaultProject;
                if (project == null) {
                    project = open();
                    myDefaultProject = project;
                }
            }
        }
        return project;
    }

    public List<Project> opened() {
        return List.copyOf(myProjects);
    }

    /**
     * Settles, closes and disposes the project now, for a test which reopens the same directory afterwards.
     */
    public void close(Project project) throws Exception {
        try {
            closeAndDispose(project);
        }
        finally {
            myProjects.remove(project);
        }
    }

    void closeAll() throws Exception {
        Throwable failure = null;
        for (Project project : opened()) {
            try {
                if (!project.isDisposed()) {
                    closeAndDispose(project);
                }
            }
            catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                }
                else {
                    failure.addSuppressed(t);
                }
            }
            finally {
                myProjects.remove(project);
            }
        }
        if (failure instanceof Exception exception) {
            throw exception;
        }
        if (failure != null) {
            throw new AssertionError("closing the projects of " + myName + " failed", failure);
        }
    }

    /**
     * Waits until the project has nothing left to do for its indexes: no scanning queued or running, no dumb task pending or
     * running, not dumb, and the dumb mode listeners of the last transition already notified. This is the settled state a test
     * needs before it reads an index, takes a baseline of dumb mode events, or closes the project; a bare smart moment is not,
     * because the indexing a finished scan schedules starts only after it.
     */
    public static void awaitIdle(Project project) throws Exception {
        if (project.getApplication().isWriteAccessAllowed()) {
            throw new IllegalStateException("awaitIdle must not be called inside a write action");
        }
        DumbServiceImpl dumbService = (DumbServiceImpl) DumbService.getInstance(project);
        dumbService.ensureInitialDumbTaskRequiredForSmartModeSubmitted();
        CompletableFuture<?> idle = dumbService.whenIdle();
        try {
            idle.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e) {
            String description = "scanning and dumb queue must become idle";
            dumpThreads(description);
            throw new AssertionError("timed out: " + description + " [" + dumbService.describeIdleState() + "]", e);
        }
    }

    /**
     * Settles the project, then closes and disposes it the way the IDE does, so that the closing half of the indexing state -
     * the per-project dirty files queue and the persistent indexable files filter - is written to disk.
     */
    public static void closeAndDispose(Project project) throws Exception {
        awaitIdle(project);
        ProjectManager projectManager = project.getApplication().getInstance(ProjectManager.class);
        Boolean closed = projectManager
            .closeAndDisposeAsync(project, project.getUIAccess())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(closed)) {
            throw new AssertionError("the project must actually close: " + project);
        }
        waitFor("the closed project must be disposed", project::isDisposed);
    }

    private static void waitFor(String description, BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        dumpThreads(description);
        throw new AssertionError("timed out: " + description);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void dumpThreads(String description) {
        StringBuilder dump = new StringBuilder();
        dump.append("=== thread dump at timeout of: ").append(description)
            .append(" [availableProcessors=").append(Runtime.getRuntime().availableProcessors()).append("]\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            dump.append('"').append(thread.getName()).append("\" ").append(thread.getState()).append('\n');
            StackTraceElement[] frames = entry.getValue();
            for (int i = 0; i < Math.min(frames.length, 30); i++) {
                dump.append("    at ").append(frames[i]).append('\n');
            }
        }
        dump.append("=== end of thread dump\n");
        System.err.print(dump);
    }
}
