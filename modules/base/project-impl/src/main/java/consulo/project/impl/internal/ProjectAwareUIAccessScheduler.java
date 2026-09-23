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
package consulo.project.impl.internal;

import consulo.ui.ModalityState;
import consulo.ui.UIAccessScheduler;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
public class ProjectAwareUIAccessScheduler implements UIAccessScheduler {
    private final UIAccessScheduler myScheduler;
    private final ProjectAwareUIAccess myUIAccess;

    public ProjectAwareUIAccessScheduler(UIAccessScheduler scheduler, ProjectAwareUIAccess uiAccess) {
        myScheduler = scheduler;
        myUIAccess = uiAccess;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, ModalityState modalityState, long delay, TimeUnit unit) {
        return myScheduler.schedule(myUIAccess.wrap(command), modalityState, delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        return myScheduler.schedule(myUIAccess.wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        return myScheduler.schedule(myUIAccess.wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        return myScheduler.scheduleAtFixedRate(myUIAccess.wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        return myScheduler.scheduleWithFixedDelay(myUIAccess.wrap(command), initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        myScheduler.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return myScheduler.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return myScheduler.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return myScheduler.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return myScheduler.awaitTermination(timeout, unit);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        return myScheduler.submit(myUIAccess.wrap(task));
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        return myScheduler.submit(myUIAccess.wrap(task), result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        return myScheduler.submit(myUIAccess.wrap(task));
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        myScheduler.execute(myUIAccess.wrap(command));
    }
}
