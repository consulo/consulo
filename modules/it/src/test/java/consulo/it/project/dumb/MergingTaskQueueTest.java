// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.logging.Logger;
import consulo.project.MergeableQueueTask;
import consulo.project.impl.internal.MergingTaskQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(HeadlessApplicationExtension.class)
public class MergingTaskQueueTest {
    private static final Logger LOG = Logger.getInstance(MergingTaskQueueTest.class);

    private final MergingTaskQueue myQueue = new MergingTaskQueue<>();

    private void runAllTasks() {
        while (true) {
            try (MergingTaskQueue.QueuedTask<?> nextTask = myQueue.extractNextTask()) {
                if (nextTask == null) {
                    return;
                }
                nextTask.executeTask(new Exception());
            }
        }
    }

    static abstract class TaskWithEquivalentObject implements MergeableQueueTask<TaskWithEquivalentObject> {
        private final Object myEquivalenceObject;

        TaskWithEquivalentObject(Object object) {
            myEquivalenceObject = object;
        }

        @Override
        public @Nullable TaskWithEquivalentObject tryMergeWith(TaskWithEquivalentObject taskFromQueue) {
            if (taskFromQueue.getClass().equals(getClass()) && taskFromQueue.myEquivalenceObject.equals(myEquivalenceObject)) {
                return this;
            }
            return null;
        }

        @Override
        public void dispose() {
        }
    }

    static class LoggingTask implements MergeableQueueTask<LoggingTask> {
        private final @Nullable List<Integer> performLog;
        private final @Nullable List<Integer> disposeLog;
        private final BiFunction<LoggingTask, LoggingTask, @Nullable LoggingTask> tryMergeWithFn;
        private final int taskId;

        LoggingTask(int taskId, @Nullable List<Integer> performLog, @Nullable List<Integer> disposeLog) {
            this(taskId, performLog, disposeLog, (thiz, other) -> null);
        }

        LoggingTask(int taskId, @Nullable List<Integer> performLog, @Nullable List<Integer> disposeLog,
                    BiFunction<LoggingTask, LoggingTask, @Nullable LoggingTask> tryMergeWithFn) {
            this.performLog = performLog;
            this.disposeLog = disposeLog;
            this.taskId = taskId;
            this.tryMergeWithFn = tryMergeWithFn;
        }

        @Override
        public @Nullable LoggingTask tryMergeWith(LoggingTask taskFromQueue) {
            return tryMergeWithFn.apply(this, taskFromQueue);
        }

        @Override
        public void perform(ProgressIndicator indicator, Exception trace) {
            if (performLog != null) {
                performLog.add(taskId);
            }
        }

        @Override
        public void dispose() {
            if (disposeLog != null) {
                disposeLog.add(taskId);
            }
        }
    }

    @Test
    public void testEquivalentTasksAreMerged() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int taskId = i;
            myQueue.addTask(new TaskWithEquivalentObject("child") {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }

                @Override
                public void dispose() {
                    disposeLog.add(taskId);
                }
            });
        }

        runAllTasks();

        assertEquals(1, childLog.size(), "Only one child task should run, but were: " + childLog);
        assertEquals(100, disposeLog.size(), "All tasks must be disposed, but were: " + disposeLog);
    }

    @Test
    public void testCanReturnThatAsResultOfTryMerge() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            myQueue.addTask(new LoggingTask(i, childLog, disposeLog, (thiz, other) -> other /* always merges */));
        }

        runAllTasks();

        assertEquals(1, childLog.size(), "Only one child task should run, but were: " + childLog);
        assertEquals(100, disposeLog.size(), "All tasks must be disposed, but were: " + disposeLog);
    }

    @Test
    public void testCanReturnThisAsResultOfTryMerge() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            myQueue.addTask(new LoggingTask(i, childLog, disposeLog, (thiz, other) -> thiz /* always merges */));
        }

        runAllTasks();

        assertEquals(1, childLog.size(), "Only one child task should run, but were: " + childLog);
        assertEquals(100, disposeLog.size(), "All tasks must be disposed, but were: " + disposeLog);
    }

    @Test
    public void testDifferentClassesWithSameEquivalentAreNotMerged() {
        List<Integer> childLog = new ArrayList<>();
        String commonEquivalence = "child";
        TaskWithEquivalentObject taskA = new TaskWithEquivalentObject(commonEquivalence) {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
                childLog.add(1);
            }
        };

        TaskWithEquivalentObject taskB = new TaskWithEquivalentObject(commonEquivalence) {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
                childLog.add(-1);
            }
        };

        //both taskA and taskB submits the same equality object, it must run both
        myQueue.addTask(taskA);
        myQueue.addTask(taskB);
        runAllTasks();
        assertEquals(2, childLog.size(), "All tasks should run, but were: " + childLog);
    }

    @Test
    public void testNonEquivalentTasksAreNotMerged() {
        List<Integer> childLog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int taskId = i;
            myQueue.addTask(new TaskWithEquivalentObject("child" + i) {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }
            });
        }
        runAllTasks();
        assertEquals(100, childLog.size(), "Every child task are not unique, all must be executed: " + childLog);
    }

    @Test
    public void testNewTaskIsRunWhenMerged() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            int taskId = i;
            myQueue.addTask(new TaskWithEquivalentObject("child") {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }

                @Override
                public void dispose() {
                    disposeLog.add(taskId);
                }
            });
        }

        runAllTasks();
        assertEquals(Collections.singletonList(2), childLog, "The last child task should run, but were: " + childLog);
        assertEquals(3, disposeLog.size(), "All tasks must be disposed, but were: " + disposeLog);
    }

    @Test
    public void testMergedTaskIsRunWhenMerged() {
        List<String> disposeLog = new ArrayList<>();
        List<String> childLog = new ArrayList<>();

        class TaskWithId implements MergeableQueueTask<TaskWithId> {
            protected final String taskId;

            TaskWithId(String taskId) {
                this.taskId = taskId;
            }

            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
                childLog.add(taskId);
            }

            @Override
            public void dispose() {
                disposeLog.add(taskId);
            }

            @Override
            public @Nullable TaskWithId tryMergeWith(TaskWithId taskFromQueue) {
                String newId = taskFromQueue.taskId + " " + taskId;
                return new TaskWithId(newId);
            }
        }
        for (int i = 0; i < 3; i++) {
            myQueue.addTask(new TaskWithId(String.valueOf(i)));
        }

        runAllTasks();
        assertEquals(Collections.singletonList("0 1 2"), childLog, "The last child task should run, but were: " + childLog);
        assertEquals(3, disposeLog.size(), "All tasks must be disposed, but were: " + disposeLog);
    }

    @Test
    public void testCancelledTask() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();

        LoggingTask task = new LoggingTask(1, childLog, disposeLog);

        myQueue.addTask(task);
        myQueue.cancelTask(task);

        runAllTasks();
        assertEquals(Collections.emptyList(), childLog, "Cancelled task must not run " + childLog);
        assertEquals(Collections.singletonList(1), disposeLog, "Cancelled task must dispose " + disposeLog);
    }

    @Test
    public void testMergedTaskShouldDispose() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            int taskId = i;
            myQueue.addTask(new TaskWithEquivalentObject("child") {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }

                @Override
                public void dispose() {
                    disposeLog.add(taskId);
                }
            });
        }

        assertEquals(Collections.emptyList(), childLog, "No task should run by now " + childLog);
        assertEquals(Arrays.asList(0, 1), disposeLog, "older task must be disposed" + disposeLog);

        runAllTasks();

        assertEquals(Collections.singletonList(2), childLog, "The last task should only run " + childLog);
        assertEquals(Arrays.asList(0, 1, 2), disposeLog, "older task must be disposed" + disposeLog);
    }

    @Test
    public void testTasksAreDisposed() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();

        TaskWithEquivalentObject task = new TaskWithEquivalentObject("child") {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
                childLog.add(1);
            }

            @Override
            public void dispose() {
                disposeLog.add(1);
            }
        };

        myQueue.addTask(task);
        myQueue.disposePendingTasks();

        assertEquals(Collections.emptyList(), childLog, "The last task should only run " + childLog);
        assertEquals(Arrays.asList(1), disposeLog, "older task must be disposed" + disposeLog);

        runAllTasks();
        assertEquals(Collections.emptyList(), childLog, "The last task should only run " + childLog);
        assertEquals(Arrays.asList(1), disposeLog, "older task must be disposed" + disposeLog);
    }

    @Test
    public void testMergedTasksShouldNotReserveEarlierSlots() {
        List<Integer> disposeLog = new ArrayList<>();
        List<Integer> childLog = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            int taskId = i;
            myQueue.addTask(new TaskWithEquivalentObject("child") {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }

                @Override
                public void dispose() {
                    disposeLog.add(taskId);
                }
            });

            myQueue.addTask(new TaskWithEquivalentObject("boss-" + i) {
                @Override
                public void perform(ProgressIndicator indicator, Exception trace) {
                    childLog.add(-taskId);
                }

                @Override
                public void dispose() {
                    disposeLog.add(-taskId);
                }
            });
        }

        runAllTasks();
        assertEquals(Arrays.asList(-1, -2, 3, -3), childLog, "The last task should only run " + childLog);
        assertEquals(Arrays.asList(1, 2, -1, -2, 3, -3), disposeLog, "older task must be disposed " + disposeLog);
    }

    /** IDEA-241378 */
    @Test
    public void testRunningTaskShouldNotBeDisposed() {
        AtomicReference<Boolean> isDisposed = new AtomicReference<>();
        myQueue.addTask(new TaskWithEquivalentObject("any") {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
            }

            @Override
            public void dispose() {
                isDisposed.set(true);
            }
        });

        MergingTaskQueue.QueuedTask<?> task = myQueue.extractNextTask();
        myQueue.disposePendingTasks();

        assertNull(isDisposed.get());
        try {
            task.executeTask(new Exception());
            fail();
        }
        catch (ProcessCanceledException ignore) {
            //OK
        }
        finally {
            task.close();
        }
        assertEquals(Boolean.TRUE, isDisposed.get());
    }

    /** IDEA-241378 */
    @Test
    public void testRunningTaskIndicatorShouldBeCancelledOnDisposeRunningTasks() {
        CyclicBarrier b = new CyclicBarrier(2);
        AtomicReference<Boolean> isRun = new AtomicReference<>();
        myQueue.addTask(new TaskWithEquivalentObject("any") {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
                for (int i = 0; i < 2; i++) {
                    await(b);
                }
                isRun.set(indicator.isCanceled());
            }
        });

        Thread th = new Thread(() -> {
            try (MergingTaskQueue.QueuedTask<?> nextTask = myQueue.extractNextTask()) {
                nextTask.executeTask(new Exception());
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }, getClass().getName() + "-thread");
        th.setDaemon(true);
        th.start();

        await(b);
        //now the task is in the middle
        myQueue.disposePendingTasks();
        await(b);

        //now it should complete
        try {
            th.join(5_000);
        }
        catch (InterruptedException e) {
            th.interrupt();
            fail();
        }

        assertEquals(Boolean.TRUE, isRun.get());
    }

    @Test
    public void testNoDisposeLeaksOnClose() {
        AtomicBoolean myDisposeFlag = new AtomicBoolean(false);
        TaskWithEquivalentObject task = new TaskWithEquivalentObject(this) {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
            }

            @Override
            public void dispose() {
                myDisposeFlag.set(true);
            }
        };

        myQueue.addTask(task);
        myQueue.disposePendingTasks();

        assertTrue(Disposer.isDisposed(task));
        assertTrue(myDisposeFlag.get());
    }

    @Test
    public void testNoDisposeLeaksOnClose2() {
        AtomicBoolean myDisposeFlag = new AtomicBoolean(false);
        TaskWithEquivalentObject task = new TaskWithEquivalentObject(this) {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
            }

            @Override
            public void dispose() {
                myDisposeFlag.set(true);
            }
        };

        myQueue.addTask(task);
        myQueue.cancelAllTasks();
        myQueue.disposePendingTasks();

        assertTrue(Disposer.isDisposed(task));
        assertTrue(myDisposeFlag.get());
    }

    @Test
    public void testNoDisposeLeaksOnClose3() {
        AtomicBoolean myDisposeFlag = new AtomicBoolean(false);
        TaskWithEquivalentObject task = new TaskWithEquivalentObject(this) {
            @Override
            public void perform(ProgressIndicator indicator, Exception trace) {
            }

            @Override
            public void dispose() {
                myDisposeFlag.set(true);
            }
        };

        myQueue.addTask(task);
        myQueue.cancelTask(task);
        myQueue.disposePendingTasks();

        assertTrue(Disposer.isDisposed(task));
        assertTrue(myDisposeFlag.get());
    }

    private static void await(CyclicBarrier b) {
        try {
            b.await();
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }
}

