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

import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import org.jspecify.annotations.Nullable;

import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class ManualTreeExecutor implements TreeExecutor {
    private final Deque<Consumer<Boolean>> myTasks = new ConcurrentLinkedDeque<>();

    @Override
    public <T> CompletableFuture<T> execute(Tree<?> tree, Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        myTasks.add(run -> {
            if (!run) {
                result.completeExceptionally(new CancellationException());
                return;
            }

            try {
                result.complete(task.get());
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    public int getPendingCount() {
        return myTasks.size();
    }

    public boolean runNext() {
        return step(myTasks.pollFirst(), true);
    }

    public boolean runLast() {
        return step(myTasks.pollLast(), true);
    }

    public boolean cancelNext() {
        return step(myTasks.pollFirst(), false);
    }

    public int runAll() {
        int count = 0;
        while (runNext()) {
            count++;
        }
        return count;
    }

    private static boolean step(@Nullable Consumer<Boolean> task, boolean run) {
        if (task == null) {
            return false;
        }

        task.accept(run);
        return true;
    }
}
