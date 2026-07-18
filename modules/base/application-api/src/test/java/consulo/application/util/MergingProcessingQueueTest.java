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
package consulo.application.util;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.util.concurrent.coroutine.CoroutineContext;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MergingProcessingQueueTest {
    @Test
    void processesAllQueuedKeys() throws Exception {
        CountingScheduler scheduler = new CountingScheduler();
        try {
            int count = 100;
            CountDownLatch latch = new CountDownLatch(count);
            List<Integer> processed = new CopyOnWriteArrayList<>();

            MergingProcessingQueue<Integer> queue = new MergingProcessingQueue<>(concurrency(scheduler), 200) {
                @Override
                protected void process(Integer key) {
                    processed.add(key);
                    latch.countDown();
                }
            };

            for (int i = 0; i < count; i++) {
                queue.queueAdd(i);
            }

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(processed).hasSize(count);
        }
        finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void coalescesBurstIntoSingleScheduledRun() throws Exception {
        CountingScheduler scheduler = new CountingScheduler();
        try {
            int count = 100;
            CountDownLatch latch = new CountDownLatch(count);

            MergingProcessingQueue<Integer> queue = new MergingProcessingQueue<>(concurrency(scheduler), 200) {
                @Override
                protected void process(Integer key) {
                    latch.countDown();
                }
            };

            for (int i = 0; i < count; i++) {
                queue.queueAdd(i);
            }

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(scheduler.scheduleCount.get()).isEqualTo(1);
        }
        finally {
            scheduler.shutdownNow();
        }
    }

    private static ApplicationConcurrency concurrency(ScheduledExecutorService scheduler) {
        return new ApplicationConcurrency() {
            @Override
            public CoroutineContext coroutineContext() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Executor executor() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExecutorService getExecutorService() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ScheduledExecutorService getScheduledExecutorService() {
                return scheduler;
            }

            @Override
            public ScheduledExecutorService createBoundedScheduledExecutorService(String name, int maxThreads) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExecutorService createBoundedApplicationPoolExecutor(String name, Executor backendExecutor, int maxThreads) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExecutorService createBoundedApplicationPoolExecutor(String name, Executor backendExecutor, int maxThreads, Disposable parentDisposable) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ExecutorService createBoundedApplicationPoolExecutor(String name, int maxThreads, Disposable parentDisposable) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class CountingScheduler implements ScheduledExecutorService {
        private final ScheduledExecutorService myDelegate = Executors.newSingleThreadScheduledExecutor();
        private final AtomicInteger scheduleCount = new AtomicInteger();

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduleCount.incrementAndGet();
            return myDelegate.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return myDelegate.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return myDelegate.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return myDelegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        @Override
        public void shutdown() {
            myDelegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return myDelegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return myDelegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return myDelegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return myDelegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return myDelegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return myDelegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return myDelegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return myDelegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return myDelegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return myDelegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return myDelegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            myDelegate.execute(command);
        }
    }
}
