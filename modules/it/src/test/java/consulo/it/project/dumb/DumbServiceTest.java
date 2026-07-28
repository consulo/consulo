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
package consulo.it.project.dumb;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.it.HeadlessApplicationExtension;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.ui.UIAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives real dumb mode cycles against a real opened project.
 * <p>
 * Guards two properties of {@code DumbServiceImpl}:
 * <ul>
 * <li>{@link DumbModeListenerBackgroundable} is published inside a write action, so a listener that
 * takes a write action itself nests instead of parking in {@code ReadMostlyRWLock.writeLock};</li>
 * <li>the {@code myState} machine and the dumb counter stay in agreement across repeated cycles.
 * When they diverge, {@code updateFinished} decrements the counter below zero and fails with
 * {@code AssertionError: Unbalanced dumb counter decrement}.</li>
 * </ul>
 * A cycle only queues its task once the previous cycle's {@code exitDumbMode} has been observed, so
 * consecutive cycles cannot coalesce into a single dumb mode and the enter/exit events are expected
 * to pair up exactly.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class DumbServiceTest {
    private static final int CYCLES = 20;
    private static final long TIMEOUT_SECONDS = 60;

    private static final String ENTERED = "entered";
    private static final String EXITED = "exited";

    private record Event(String name, boolean underWriteAction) {
    }

    @Test
    public void queueTaskFromUiThreadEntersAndExitsDumbMode(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager);
        DumbService dumbService = DumbService.getInstance(project);
        BlockingQueue<Event> events = subscribe(application, project, dumbService);

        UIAccess uiAccess = application.getLastUIAccess();
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            CountDownLatch performed = new CountDownLatch(1);

            boolean dumbRightAfterQueue = uiAccess.<Boolean>giveAsync(() -> {
                dumbService.queueTask(task(performed));
                return dumbService.isDumb();
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(dumbRightAfterQueue)
                .as("queueTask from the UI thread must enter dumb mode immediately, cycle %d", cycle)
                .isTrue();

            take(events, ENTERED, cycle);
            awaitTask(performed, cycle);
            take(events, EXITED, cycle);
        }

        assertThat(events).as("no unpaired dumb mode events left").isEmpty();
    }

    @Test
    public void queueTaskFromBackgroundThreadEntersAndExitsDumbMode(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager);
        DumbService dumbService = DumbService.getInstance(project);
        BlockingQueue<Event> events = subscribe(application, project, dumbService);

        assertThat(application.isDispatchThread())
            .as("this test must queue from a non-UI thread to reach the background path")
            .isFalse();

        for (int cycle = 0; cycle < CYCLES; cycle++) {
            CountDownLatch performed = new CountDownLatch(1);

            dumbService.queueTask(task(performed));

            take(events, ENTERED, cycle);
            awaitTask(performed, cycle);
            take(events, EXITED, cycle);
        }

        assertThat(events).as("no unpaired dumb mode events left").isEmpty();
    }

    @Test
    public void queueTaskInsideWriteActionPublishesBeforeItReturns(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager);
        DumbService dumbService = DumbService.getInstance(project);
        BlockingQueue<Event> events = subscribe(application, project, dumbService);

        CountDownLatch performed = new CountDownLatch(1);
        AtomicBoolean dumbOnReturn = new AtomicBoolean();
        AtomicBoolean publishedOnReturn = new AtomicBoolean();

        WriteAction.run(() -> {
            dumbService.queueTask(task(performed));
            dumbOnReturn.set(dumbService.isDumb());
            publishedOnReturn.set(!events.isEmpty());
        });

        assertThat(dumbOnReturn.get()).as("queueTask must enter dumb mode before it returns").isTrue();
        assertThat(publishedOnReturn.get())
            .as("entering dumb mode must be published before queueTask returns, or isDumb and the listeners disagree")
            .isTrue();

        take(events, ENTERED, 0);
        awaitTask(performed, 0);
        take(events, EXITED, 0);

        assertThat(events).as("no unpaired dumb mode events left").isEmpty();
    }

    private static DumbModeTask task(CountDownLatch performed) {
        return new DumbModeTask() {
            @Override
            public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
                performed.countDown();
            }
        };
    }

    /**
     * Subscribes once the project has settled into smart mode, so the indexing started by the open flow
     * cannot leak an unpaired {@code exitDumbMode} into the recorded events.
     */
    private static BlockingQueue<Event> subscribe(Application application, Project project, DumbService dumbService)
        throws InterruptedException {
        BlockingQueue<Event> events = new LinkedBlockingQueue<>();
        project.getMessageBus().connect().subscribe(
            DumbModeListenerBackgroundable.class,
            new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    events.add(new Event(ENTERED, application.isWriteAccessAllowed()));
                }

                @Override
                public void exitDumbMode() {
                    events.add(new Event(EXITED, application.isWriteAccessAllowed()));
                }
            }
        );

        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("project must reach smart mode after open")
            .isTrue();
        events.clear();
        return events;
    }

    private static Project openProject(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-dumb-service");
        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(project).isNotNull();
        return project;
    }

    private static void awaitTask(CountDownLatch performed, int cycle) throws InterruptedException {
        assertThat(performed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("dumb task must be performed, cycle %d", cycle)
            .isTrue();
    }

    private static void take(BlockingQueue<Event> events, String expected, int cycle) throws InterruptedException {
        Event event = events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(event).as("%s must be published, cycle %d", expected, cycle).isNotNull();
        assertThat(event.name())
            .as("dumb mode events must strictly alternate, cycle %d", cycle)
            .isEqualTo(expected);
        assertThat(event.underWriteAction())
            .as("%s must be published inside a write action, cycle %d", expected, cycle)
            .isTrue();
    }
}
