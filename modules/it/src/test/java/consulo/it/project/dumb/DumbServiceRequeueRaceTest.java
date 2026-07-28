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
import consulo.application.progress.ProgressIndicator;
import consulo.it.HeadlessApplicationExtension;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.DumbModeListenerBackgroundable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queues a dumb mode task from a background thread the moment the project reports itself smart again, which is the
 * boundary where leaving one dumb mode overlaps entering the next.
 * <p>
 * {@code runWhenSmart} runs its runnable as soon as the state machine says smart, so the requeue it triggers races
 * whatever the exit still has left to do. If the state reached {@code SMART} before {@code exitDumbMode} was
 * published, the {@code enteredDumbMode} of the next cycle overtakes it and the sequence repeats an event.
 * <p>
 * A single queueing thread is deliberate. Additional ones keep the task queue non-empty, so the dumb mode never ends
 * and the whole run collapses into one {@code entered}/{@code exited} pair - the transition under test never happens.
 * For the same reason the cycle waits for smart mode rather than for the task to be performed: a task queued while the
 * previous one is still running is picked up by the same dumb mode, and no transition happens at all.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class DumbServiceRequeueRaceTest {
    private static final int CYCLES = 300;
    private static final long TIMEOUT_SECONDS = 120;

    private static final String ENTERED = "entered";
    private static final String EXITED = "exited";

    /**
     * Guards against the test passing because nothing happened - if the dumb modes coalesce, the alternation check is
     * satisfied by a single pair of events and exercises no transition at all.
     */
    private static final int MINIMUM_EVENTS = 10;

    @Test
    public void requeueWhileLeavingDumbModeKeepsEventsAlternating(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager);
        DumbService dumbService = DumbService.getInstance(project);

        List<String> events = Collections.synchronizedList(new ArrayList<>());
        List<String> notUnderWriteAction = Collections.synchronizedList(new ArrayList<>());

        project.getMessageBus().connect().subscribe(
            DumbModeListenerBackgroundable.class,
            new DumbModeListenerBackgroundable() {
                @Override
                public void enteredDumbMode() {
                    record(ENTERED);
                }

                @Override
                public void exitDumbMode() {
                    record(EXITED);
                }

                private void record(String name) {
                    if (!application.isWriteAccessAllowed()) {
                        notUnderWriteAction.add(name);
                    }
                    events.add(name);
                }
            }
        );

        awaitSmart(dumbService);
        events.clear();
        notUnderWriteAction.clear();

        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        Thread queueing = new Thread(
            () -> {
                try {
                    for (int cycle = 0; cycle < CYCLES; cycle++) {
                        CountDownLatch performed = new CountDownLatch(1);
                        CountDownLatch smart = new CountDownLatch(1);

                        dumbService.queueTask(task(performed));
                        dumbService.runWhenSmart(smart::countDown);

                        if (!performed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            failures.add("gave up waiting for the task of cycle " + cycle);
                            return;
                        }
                        if (!smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            failures.add("gave up waiting for smart mode after cycle " + cycle);
                            return;
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            "dumb-requeue"
        );

        queueing.start();
        queueing.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertThat(queueing.isAlive()).as("the queueing thread must finish").isFalse();
        assertThat(failures).as("every cycle must run its task and come back to smart mode").isEmpty();

        // the exit is published in the write lock step before the one which drains the runWhenSmart queue, so this
        // callback means the last transition has been published already
        awaitSmart(dumbService);

        assertThat(notUnderWriteAction).as("every event must be published inside a write action").isEmpty();
        assertThat(dumbService.isDumb()).as("the project must be smart once every task is done").isFalse();
        assertAlternating(events);
    }

    /**
     * Starts from {@link #EXITED} so that the recorded sequence has to open with an enter and close with an exit.
     */
    private static void assertAlternating(List<String> events) {
        assertThat(events)
            .as("the dumb modes coalesced, so no transition was exercised - the check below would pass vacuously")
            .hasSizeGreaterThanOrEqualTo(MINIMUM_EVENTS);

        String previous = EXITED;
        for (int index = 0; index < events.size(); index++) {
            String event = events.get(index);
            assertThat(event)
                .as("dumb mode events must alternate, but %s repeats at index %d of %s", event, index, events)
                .isNotEqualTo(previous);
            previous = event;
        }

        assertThat(previous).as("the last event must be an exit, in %s", events).isEqualTo(EXITED);
    }

    private static DumbModeTask task(CountDownLatch performed) {
        return new DumbModeTask() {
            @Override
            public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
                performed.countDown();
            }
        };
    }

    private static void awaitSmart(DumbService dumbService) throws InterruptedException {
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();
    }

    private static Project openProject(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-dumb-service-requeue");
        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(project).isNotNull();
        return project;
    }
}
