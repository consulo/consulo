package consulo.it.application;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.it.HeadlessApplicationExtension;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cancelling a coroutine whose {@link ReadLock} step waits behind a running write action finishes the coroutine
 * and wakes the waiting thread at once, and the step never runs its body.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ReadLockCancelTest {
    @Test
    public void cancelledWhileWaitingSkipsBody(Application application) throws Exception {
        AtomicBoolean bodyRan = new AtomicBoolean();
        AtomicInteger activeTasks = new AtomicInteger();
        ExecutorService executor = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        WriteHolder write = WriteHolder.start();
        try {
            CoroutineContext context = CoroutineContext.of(command -> {
                activeTasks.incrementAndGet();
                executor.execute(() -> {
                    try {
                        command.run();
                    }
                    finally {
                        activeTasks.decrementAndGet();
                    }
                });
            }, scheduler);
            context.putCopyableUserData(Application.KEY, application);

            CoroutineScope scope = CoroutineScope.of(context);
            Continuation<String> continuation = runReadLockStep(scope, bodyRan);

            Thread.sleep(300);
            assertThat(bodyRan).as("the read must wait while the write action runs").isFalse();

            scope.cancel();

            assertThat(continuation.await(5, TimeUnit.SECONDS)).as("cancel must finish the coroutine while the write still runs").isTrue();
            assertThat(continuation.isCancelled()).isTrue();
            assertThat(scope.await(5, TimeUnit.SECONDS)).as("cancel must finish the scope while the write still runs").isTrue();
            assertThat(waitUntil(() -> activeTasks.get() == 0, 5_000))
                .as("cancel must wake the thread waiting for the read lock while the write still runs")
                .isTrue();
            assertThat(write.isHeld()).as("the write action is still running").isTrue();

            write.release();
            Thread.sleep(500);
            assertThat(bodyRan).as("a cancelled coroutine must not run the read body after the write").isFalse();
        }
        finally {
            write.release();
            executor.shutdownNow();
            scheduler.shutdownNow();
        }

        assertWriteLockIsFree();
    }

    @Test
    public void notCancelledRunsBodyAfterWrite(Application application) throws Exception {
        AtomicBoolean bodyRan = new AtomicBoolean();
        WriteHolder write = WriteHolder.start();
        Continuation<String> continuation;
        try {
            CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
            continuation = runReadLockStep(scope, bodyRan);

            Thread.sleep(300);
            assertThat(bodyRan).as("the read must wait while the write action runs").isFalse();

            write.release();

            assertThat(continuation.await(10, TimeUnit.SECONDS)).as("the step must finish after the write").isTrue();
        }
        finally {
            write.release();
        }

        assertThat(bodyRan).as("the read body runs once the write ends").isTrue();
        assertThat(continuation.getResult()).isEqualTo("body");
        assertWriteLockIsFree();
    }

    private static Continuation<String> runReadLockStep(CoroutineScope scope, AtomicBoolean bodyRan) throws InterruptedException {
        CountDownLatch entered = new CountDownLatch(1);
        Coroutine<Object, String> coroutine = Coroutine
            .first(CodeExecution.<Object, Object>apply(o -> {
                entered.countDown();
                return o;
            }))
            .then(ReadLock.<Object, String>apply(o -> {
                bodyRan.set(true);
                return "body";
            }));

        Continuation<String> continuation = coroutine.runAsync(scope, null);
        assertThat(entered.await(10, TimeUnit.SECONDS)).as("the coroutine must start").isTrue();
        return continuation;
    }

    private static boolean waitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            Thread.sleep(20);
        }
        return true;
    }

    private static void assertWriteLockIsFree() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Thread thread = new Thread(() -> WriteAction.run(done::countDown), "ReadLockCancelTest-write-check");
        thread.start();
        assertThat(done.await(10, TimeUnit.SECONDS)).as("no read lock may be left behind").isTrue();
    }

    private static final class WriteHolder {
        private final CountDownLatch myRelease = new CountDownLatch(1);
        private final Thread myThread;

        private WriteHolder(CountDownLatch held) {
            myThread = new Thread(() -> WriteAction.run(() -> {
                held.countDown();
                try {
                    myRelease.await();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }), "ReadLockCancelTest-write");
        }

        static WriteHolder start() throws InterruptedException {
            CountDownLatch held = new CountDownLatch(1);
            WriteHolder holder = new WriteHolder(held);
            holder.myThread.start();
            assertThat(held.await(10, TimeUnit.SECONDS)).as("the write action must start").isTrue();
            return holder;
        }

        boolean isHeld() {
            return myRelease.getCount() > 0 && myThread.isAlive();
        }

        void release() throws InterruptedException {
            myRelease.countDown();
            myThread.join(10_000);
        }
    }
}
