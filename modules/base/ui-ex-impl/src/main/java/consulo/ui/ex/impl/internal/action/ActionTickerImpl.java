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
package consulo.ui.ex.impl.internal.action;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.ex.internal.ActionTicker;
import consulo.ui.ex.internal.TimerListener;
import consulo.util.collection.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceImpl
public class ActionTickerImpl implements ActionTicker, Disposable {
    private static final Logger LOG = Logger.getInstance(ActionTickerImpl.class);

    private static final int TIMER_DELAY = 500;
    private static final int DEACTIVATED_TIMER_DELAY = 5000;
    private static final int UPDATE_DELAY_AFTER_TYPING = 500;

    private record Registration(UIAccess uiAccess, TimerListener listener) {
    }

    private final List<Registration> myRegistrations = Lists.newLockFreeCopyOnWriteList();

    private final Application myApplication;
    private final ApplicationConcurrency myConcurrency;

    private volatile long myLastTimeEditorWasTypedIn;
    private volatile ScheduledFuture<?> myFuture;
    private volatile int myDelay = TIMER_DELAY;

    /**
     * Read and written only from the scheduler thread.
     */
    private int myLastActivityCount;

    @Inject
    public ActionTickerImpl(Application application, ApplicationConcurrency concurrency) {
        myApplication = application;
        myConcurrency = concurrency;

        application.getMessageBus().connect(this).subscribe(ApplicationActivationListener.class, new ApplicationActivationListener() {
            @Override
            public void applicationActivated(IdeFrame ideFrame) {
                setDelay(TIMER_DELAY);
            }

            @Override
            public void applicationDeactivated(IdeFrame ideFrame) {
                setDelay(DEACTIVATED_TIMER_DELAY);
            }
        });
    }

    @Override
    public Disposable addListener(UIAccess uiAccess, TimerListener listener) {
        Registration registration = new Registration(uiAccess, listener);
        myRegistrations.add(registration);
        start();
        return () -> myRegistrations.remove(registration);
    }

    @Override
    public void notifyEditorTyping() {
        myLastTimeEditorWasTypedIn = System.currentTimeMillis();
    }

    private synchronized void start() {
        if (myFuture != null) {
            return;
        }
        myFuture = schedule(myDelay);
    }

    private synchronized void setDelay(int delay) {
        if (myDelay == delay) {
            return;
        }
        myDelay = delay;
        ScheduledFuture<?> future = myFuture;
        if (future != null) {
            future.cancel(false);
            myFuture = schedule(delay);
        }
    }

    private ScheduledFuture<?> schedule(int delay) {
        return myConcurrency.getScheduledExecutorService()
            .scheduleWithFixedDelay(this::tick, delay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs on the scheduler thread. Nothing is posted to any UI unless there is something to update, so an idle
     * application produces no UI events at all.
     */
    private void tick() {
        if (myLastTimeEditorWasTypedIn + UPDATE_DELAY_AFTER_TYPING > System.currentTimeMillis()) {
            return;
        }

        int lastActivityCount = myLastActivityCount;
        myLastActivityCount = ActivityTracker.getInstance().getCount();
        if (myLastActivityCount == lastActivityCount) {
            return;
        }

        Map<UIAccess, List<TimerListener>> byUIAccess = new LinkedHashMap<>();
        for (Registration registration : myRegistrations) {
            UIAccess uiAccess = registration.uiAccess();
            if (!uiAccess.isValid()) {
                // the UI this listener was bound to is gone (closed window, ended web session)
                myRegistrations.remove(registration);
                continue;
            }
            byUIAccess.computeIfAbsent(uiAccess, it -> new ArrayList<>()).add(registration.listener());
        }

        byUIAccess.forEach((uiAccess, listeners) -> uiAccess.give(() -> {
            for (TimerListener listener : listeners) {
                runListenerAction(listener);
            }
        }));
    }

    /**
     * Runs on the UI thread of the listener's {@link UIAccess} - {@link ModalityState#dominates} is meaningful only
     * there.
     */
    private void runListenerAction(TimerListener listener) {
        ModalityState modalityState = listener.getModalityState();
        if (modalityState == null) {
            return;
        }
        LOG.debug("notify ", listener);
        if (!myApplication.getCurrentModalityState().dominates(modalityState)) {
            try {
                listener.run();
            }
            catch (ProcessCanceledException ignored) {
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public synchronized void dispose() {
        ScheduledFuture<?> future = myFuture;
        if (future != null) {
            future.cancel(false);
            myFuture = null;
        }
        myRegistrations.clear();
    }
}
