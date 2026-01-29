//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine;

import consulo.util.concurrent.coroutine.internal.RunLock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The context for the execution of {@link Coroutine Coroutines}.
 *
 * @author eso
 */
public final class CoroutineContext extends CoroutineEnvironment {
    private final Executor executor;

    private final ScheduledExecutorService scheduler;

    private final AtomicLong runningScopes = new AtomicLong();

    private final RunLock scopeLock = new RunLock();

    private CountDownLatch scopesFinishedSignal = new CountDownLatch(1);


    /**
     * Creates a new instance with a specific coroutine executor and scheduler.
     * The latter will be used to execute timed coroutine steps.
     *
     * @param executor  The coroutine executor
     * @param scheduler The scheduled executor service
     */
    public CoroutineContext(Executor executor, ScheduledExecutorService scheduler) {
        this.executor = executor;
        this.scheduler = scheduler;
    }

    /**
     * Blocks until the coroutines of all {@link CoroutineScope scopes} in this
     * context have finished execution. If no coroutines are running or all have
     * finished execution already this method returns immediately.
     */
    public void awaitAllScopes() {
        if (scopesFinishedSignal != null) {
            try {
                scopesFinishedSignal.await();
            }
            catch (InterruptedException e) {
                throw new CoroutineException(e);
            }
        }
    }

    /**
     * Returns the executor to be used for the execution of the steps of a
     * {@link Coroutine}.
     *
     * @return The coroutine executor for this context
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Returns the executor to be used for the execution of timed steps in a
     * {@link Coroutine}. If no scheduler has been set in the constructor or
     * created before a new instance with a pool size of 1 will be created by
     * invoking {@link Executors#newScheduledThreadPool(int)}.
     *
     * @return The coroutine scheduler for this context
     */
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * Returns the number of currently active {@link CoroutineScope scopes}.
     * This will only be a momentary value as the execution of the coroutines in
     * the scopes happens asynchronously and some coroutines may finish while
     * querying this count.
     *
     * @return The number of running coroutines
     */
    public long getScopeCount() {
        return runningScopes.get();
    }

    /**
     * Notifies this context that a {@link CoroutineScope} has finished
     * executing all coroutines.
     *
     * @param scope The finished scope
     */
    void scopeFinished(CoroutineScope scope) {
        if (runningScopes.decrementAndGet() == 0) {
            scopeLock.runLocked(() -> scopesFinishedSignal.countDown());
        }
    }

    /**
     * Notifies this context that a {@link CoroutineScope} has been launched.
     *
     * @param scope The launched scope
     */
    void scopeLaunched(CoroutineScope scope) {
        if (runningScopes.incrementAndGet() == 1) {
            scopeLock.runLocked(() -> {
                if (scopesFinishedSignal.getCount() == 0) {
                    scopesFinishedSignal = new CountDownLatch(1);
                }
            });
        }
    }
}
