// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * Implementation of {@link MultiThreadSearcher.Listener} which decrease events rate and raise batch updates
 * each {@code throttlingDelay} milliseconds.
 * <br>
 * Not thread-safe and should be notified only in EDT
 */
class ThrottlingListenerWrapper implements MultiThreadSearcher.Listener {
    public final int myThrottlingDelay;

    private final MultiThreadSearcher.Listener myDelegateListener;
    private final Executor myDelegateExecutor;

    private final Buffer myBuffer = new Buffer();
    private final BiConsumer<List<SearchEverywhereFoundElementInfo>, List<SearchEverywhereFoundElementInfo>> myFlushConsumer;

    private final Alarm flushAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private boolean flushScheduled;

    ThrottlingListenerWrapper(int throttlingDelay, MultiThreadSearcher.Listener delegateListener, Executor delegateExecutor) {
        myThrottlingDelay = throttlingDelay;
        myDelegateListener = delegateListener;
        myDelegateExecutor = delegateExecutor;

        myFlushConsumer = (added, removed) -> {
            if (!added.isEmpty()) {
                myDelegateExecutor.execute(() -> myDelegateListener.elementsAdded(added));
            }
            if (!removed.isEmpty()) {
                myDelegateExecutor.execute(() -> myDelegateListener.elementsRemoved(removed));
            }
        };
    }

    @RequiredUIAccess
    public void clearBuffer() {
        UIAccess.assertIsUIThread();
        myBuffer.clear();
        cancelScheduledFlush();
    }

    @Override
    @RequiredUIAccess
    public void elementsAdded(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list) {
        UIAccess.assertIsUIThread();
        myBuffer.addEvent(new Event(Event.ADD, list));
        scheduleFlushBuffer();
    }

    @Override
    @RequiredUIAccess
    public void elementsRemoved(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list) {
        UIAccess.assertIsUIThread();
        myBuffer.addEvent(new Event(Event.REMOVE, list));
        scheduleFlushBuffer();
    }

    @Override
    @RequiredUIAccess
    public void searchFinished(@Nonnull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
        UIAccess.assertIsUIThread();
        myBuffer.flush(myFlushConsumer);
        myDelegateExecutor.execute(() -> myDelegateListener.searchFinished(hasMoreContributors));
        cancelScheduledFlush();
    }

    @RequiredUIAccess
    private void scheduleFlushBuffer() {
        UIAccess.assertIsUIThread();

        @RequiredUIAccess
        Runnable flushTask = () -> {
            UIAccess.assertIsUIThread();
            if (!flushScheduled) {
                return;
            }
            flushScheduled = false;
            myBuffer.flush(myFlushConsumer);
        };

        if (!flushScheduled) {
            flushAlarm.addRequest(flushTask, myThrottlingDelay);
            flushScheduled = true;
        }
    }

    @RequiredUIAccess
    private void cancelScheduledFlush() {
        UIAccess.assertIsUIThread();
        flushAlarm.cancelAllRequests();
        flushScheduled = false;
    }

    private static class Event {
        static final int REMOVE = 0;
        static final int ADD = 1;

        final int type;
        final List<? extends SearchEverywhereFoundElementInfo> items;

        Event(int type, List<? extends SearchEverywhereFoundElementInfo> items) {
            this.type = type;
            this.items = items;
        }
    }

    private static class Buffer {
        private final Queue<Event> myQueue = new ArrayDeque<>();

        public void addEvent(Event event) {
            myQueue.add(event);
        }

        public void flush(BiConsumer<? super List<SearchEverywhereFoundElementInfo>, ? super List<SearchEverywhereFoundElementInfo>> consumer) {
            List<SearchEverywhereFoundElementInfo> added = new ArrayList<>();
            List<SearchEverywhereFoundElementInfo> removed = new ArrayList<>();
            myQueue.forEach(event -> {
                if (event.type == Event.ADD) {
                    added.addAll(event.items);
                }
                else {
                    event.items.forEach(removing -> {
                        if (!added.removeIf(existing -> existing.getContributor() == removing.getContributor() && existing.getElement() == removing.getElement())) {
                            removed.add(removing);
                        }
                    });
                }
            });
            myQueue.clear();
            consumer.accept(added, removed);
        }

        public void clear() {
            myQueue.clear();
        }
    }


}
