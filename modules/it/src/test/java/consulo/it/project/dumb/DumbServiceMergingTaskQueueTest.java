// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import consulo.it.HeadlessApplicationExtension;
import consulo.project.DumbModeTask;
import consulo.project.impl.internal.DumbServiceMergingTaskQueue;
import consulo.project.impl.internal.MergingTaskQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(HeadlessApplicationExtension.class)
public class DumbServiceMergingTaskQueueTest {
    private final DumbServiceMergingTaskQueue myQueue = new DumbServiceMergingTaskQueue();

    private void runAllTasks() {
        while (true) {
            try (MergingTaskQueue.QueuedTask<DumbModeTask> nextTask = myQueue.extractNextTask()) {
                if (nextTask == null) {
                    return;
                }
                nextTask.executeTask(new Exception());
            }
        }
    }

    @Test
    public void testDumbModeTasksAreNotMergedByDefault() {
        List<Integer> childLog = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int taskId = i;
            myQueue.addTask(new DumbModeTask() {
                @Override
                public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
                    childLog.add(taskId);
                }
            });
        }
        runAllTasks();
        assertEquals(100, childLog.size(), "Every child task are not unique, all must be executed: " + childLog);
    }
}
