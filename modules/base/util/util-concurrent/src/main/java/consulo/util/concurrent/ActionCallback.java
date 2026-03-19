/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.util.concurrent;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ActionCallback {
    private static final Logger LOG = LoggerFactory.getLogger(ActionCallback.class);

    public static final ActionCallback DONE = new Done();
    public static final ActionCallback REJECTED = new Rejected();

    private final ExecutionCallback myDone;
    private final ExecutionCallback myRejected;

    protected @Nullable String myError = null;
    protected @Nullable Throwable myThrowable = null;

    private final @Nullable String myName;

    public ActionCallback() {
        this(null);
    }

    public ActionCallback(@Nullable String name) {
        myName = name;
        myDone = new ExecutionCallback();
        myRejected = new ExecutionCallback();
    }

    public ActionCallback(int countToDone) {
        this(null, countToDone);
    }

    public ActionCallback(@Nullable String name, int countToDone) {
        myName = name;

        assert countToDone >= 0 : "count=" + countToDone;

        int count = countToDone >= 1 ? countToDone : 1;

        myDone = new ExecutionCallback(count);
        myRejected = new ExecutionCallback();

        if (countToDone < 1) {
            setDone();
        }
    }

    public void setDone() {
        if (myDone.setExecuted()) {
            myRejected.clear();
            freeResources();
        }
    }

    public boolean isDone() {
        return myDone.isExecuted();
    }

    public boolean isRejected() {
        return myRejected.isExecuted();
    }

    public boolean isProcessed() {
        return isDone() || isRejected();
    }

    public void setRejected() {
        if (isProcessed()) {
            LOG.warn("can't change state after set state", new UnsupportedOperationException());
        }

        if (myRejected.setExecuted()) {
            myDone.clear();
            freeResources();
        }
    }

    protected void freeResources() {
    }

    public ActionCallback reject(String error) {
        myError = error;
        setRejected();
        return this;
    }

    public ActionCallback rejectWithThrowable(Throwable error) {
        myThrowable = error;
        setRejected();
        return this;
    }

    public @Nullable String getError() {
        return myError;
    }

    public ActionCallback doWhenDone(Runnable runnable) {
        myDone.doWhenExecuted(runnable);
        return this;
    }

    public final ActionCallback doWhenRejected(Runnable runnable) {
        myRejected.doWhenExecuted(runnable);
        return this;
    }

    public final ActionCallback doWhenRejectedButNotThrowable(Runnable runnable) {
        myRejected.doWhenExecuted(() -> {
            if (myThrowable == null) {
                runnable.run();
            }
        });
        return this;
    }

    public final ActionCallback doWhenRejectedWithThrowable(Consumer<Throwable> consumer) {
        myRejected.doWhenExecuted(() -> {
            if (myThrowable != null) {
                consumer.accept(myThrowable);
            }
        });
        return this;
    }

    public final ActionCallback doWhenRejected(Consumer<String> consumer) {
        myRejected.doWhenExecuted(() -> consumer.accept(myError));
        return this;
    }

    public ActionCallback doWhenProcessed(Runnable runnable) {
        doWhenDone(runnable);
        doWhenRejected(runnable);
        return this;
    }

    public final ActionCallback notifyWhenDone(ActionCallback child) {
        return doWhenDone(child.createSetDoneRunnable());
    }

    public final ActionCallback notifyWhenRejected(ActionCallback child) {
        return doWhenRejected(() -> child.reject(StringUtil.notNullize(myError)));
    }

    public ActionCallback notify(ActionCallback child) {
        return doWhenDone(child.createSetDoneRunnable()).notifyWhenRejected(child);
    }

    public final ActionCallback processOnDone(Runnable runnable, boolean requiresDone) {
        if (requiresDone) {
            return doWhenDone(runnable);
        }
        runnable.run();
        return this;
    }

    public static class Done extends ActionCallback {
        public Done() {
            setDone();
        }
    }

    public static class Rejected extends ActionCallback {
        public Rejected() {
            setRejected();
        }
    }

    @Override
    public String toString() {
        String name = myName != null ? myName : super.toString();
        return name + " done=[" + myDone + "] rejected=[" + myRejected + "]";
    }

    public static class Chunk {
        private final Set<ActionCallback> myCallbacks = new LinkedHashSet<>();

        public void add(ActionCallback callback) {
            myCallbacks.add(callback);
        }

        public ActionCallback create() {
            if (myCallbacks.isEmpty()) {
                return new Done();
            }

            ActionCallback result = new ActionCallback(myCallbacks.size());
            Runnable doneRunnable = result.createSetDoneRunnable();
            for (ActionCallback each : myCallbacks) {
                each.doWhenDone(doneRunnable).notifyWhenRejected(result);
            }
            return result;
        }

        public ActionCallback getWhenProcessed() {
            ActionCallback result = new ActionCallback(myCallbacks.size());
            Runnable setDoneRunnable = result.createSetDoneRunnable();
            for (ActionCallback each : myCallbacks) {
                each.doWhenProcessed(setDoneRunnable);
            }
            return result;
        }
    }

    public Runnable createSetDoneRunnable() {
        return this::setDone;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    /**
     * @deprecated use {@link #notifyWhenRejected(ActionCallback)}
     */ public Runnable createSetRejectedRunnable() {
        return this::setRejected;
    }

    public boolean waitFor(long msTimeout) {
        if (isProcessed()) {
            return true;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        doWhenProcessed(countDownLatch::countDown);

        try {
            if (msTimeout == -1) {
                countDownLatch.await();
            }
            else if (!countDownLatch.await(msTimeout, TimeUnit.MILLISECONDS)) {
                reject("Time limit exceeded");
                return false;
            }
        }
        catch (InterruptedException e) {
            reject(StringUtil.notNullize(e.getMessage()));
            return false;
        }
        return true;
    }
}