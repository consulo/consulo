// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.AccessToken;
import consulo.application.internal.ProgressSuspender;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.localize.ProjectLocalize;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class MergingQueueGuiSuspender {
    private static final Logger LOG = Logger.getInstance(MergingQueueGuiSuspender.class);

    private volatile @Nullable ProgressSuspender myCurrentSuspender;
    private final List<LocalizeValue> myRequestedSuspensions = Lists.newLockFreeCopyOnWriteList();

    public void suspendAndRun(LocalizeValue activityName, Runnable activity) {
        try (AccessToken ignored = heavyActivityStarted(activityName)) {
            activity.run();
        }
    }

    public AccessToken heavyActivityStarted(LocalizeValue activityName) {
        LocalizeValue reason = ProjectLocalize.dumbServiceIndexingPausedDueTo(activityName);
        synchronized (myRequestedSuspensions) {
            myRequestedSuspensions.add(reason);
        }
        suspendCurrentTask(reason);
        return new AccessToken() {
            @Override
            public void finish() {
                synchronized (myRequestedSuspensions) {
                    myRequestedSuspensions.remove(reason);
                }
                resumeAutoSuspendedTask(reason);
            }
        };
    }

    public void resumeProgressIfPossible() {
        ProgressSuspender suspender = myCurrentSuspender;
        if (suspender != null && suspender.isSuspended()) {
            suspender.resumeProcess();
        }
    }

    public <T> T setCurrentSuspenderAndSuspendIfRequested(@Nullable ProgressSuspender suspender, Supplier<T> runnable) {
        if (suspender == null) {
            return runnable.get();
        }

        LOG.assertTrue(myCurrentSuspender == null, "Already suspended in another thread, or recursive invocation.");
        try {
            myCurrentSuspender = suspender;
            suspendIfRequested(suspender);
            return runnable.get();
        }
        finally {
            LOG.assertTrue(myCurrentSuspender == suspender, "Suspender has changed unexpectedly");
            myCurrentSuspender = null;
        }
    }

    private void resumeAutoSuspendedTask(LocalizeValue reason) {
        ProgressSuspender currentSuspender = myCurrentSuspender;
        if (currentSuspender != null && currentSuspender.isSuspended()) {
            currentSuspender.resumeProcess();
            suspendIfRequested(currentSuspender); // take the following reason from the queue (if any)
        }
    }

    private void suspendIfRequested(ProgressSuspender suspender) {
        LocalizeValue suspendedReason;
        synchronized (myRequestedSuspensions) {
            suspendedReason = ContainerUtil.getLastItem(myRequestedSuspensions);
        }
        if (suspendedReason != null) {
            suspender.suspendProcess(suspendedReason);
        }
    }

    private void suspendCurrentTask(LocalizeValue reason) {
        ProgressSuspender currentSuspender = myCurrentSuspender;
        if (currentSuspender != null && !currentSuspender.isSuspended()) {
            currentSuspender.suspendProcess(reason);
        }
    }
}
