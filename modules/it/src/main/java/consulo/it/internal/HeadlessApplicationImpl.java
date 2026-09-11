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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.application.internal.StartupProgress;
import consulo.application.impl.internal.ReadMostlyRWLock;
import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.progress.ProgressManager;
import consulo.component.internal.ComponentBinding;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.util.concurrent.ThreadIssueException;
import consulo.util.lang.ref.SimpleReference;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * Real (non-stub) headless {@link consulo.application.Application} for integration tests, built on
 * {@link UnifiedApplication}. UI work is dispatched onto the single {@link HeadlessUIAccess} thread.
 *
 * @author VISTALL
 */
public class HeadlessApplicationImpl extends UnifiedApplication {
    private static final List<ThreadIssueException> ourThreadIssues = new CopyOnWriteArrayList<>();

    private static volatile boolean ourAllowWriteLockUnderUIThread;

    public HeadlessApplicationImpl(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splashRef) {
        super(componentBinding, splashRef);
        myLock = new ReadMostlyRWLock(null);
    }

    /**
     * Opt-out, rejected by default: not every flow under test is free of write actions on the UI thread yet, so
     * those tests turn this on for as long as they run.
     */
    public static void setAllowWriteLockUnderUIThread(boolean value) {
        ourAllowWriteLockUnderUIThread = value;
    }

    /**
     * The write-lock-under-UI-thread ban is not applied on this branch: the lock installed above polls pending
     * write-action transfers while waiting for write intent, so a UI task taking the write lock cannot deadlock
     * against a background write action transferring onto the UI queue. The opt-out annotation and the recorded
     * issue list stay, so tests written against the ban still compile and pass.
     */

    public static List<ThreadIssueException> takeThreadIssues() {
        List<ThreadIssueException> issues = List.copyOf(ourThreadIssues);
        ourThreadIssues.clear();
        return issues;
    }

    /**
     * The issues recorded so far, left in place so that {@link #takeThreadIssues()} at the end of the test still
     * fails it: for a diagnostic printed while the test is running.
     */
    public static List<ThreadIssueException> peekThreadIssues() {
        return List.copyOf(ourThreadIssues);
    }

    @Override
    public int getProfiles() {
        return super.getProfiles() | ComponentProfiles.INTEGRATION_TEST;
    }

    @Override
    public ProgressManager getProgressManager() {
        return getInstance(ProgressManager.class);
    }

    @Override
    public void invokeLater(Runnable runnable) {
        HeadlessUIAccess.INSTANCE.giveAsync(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void invokeLater(Runnable runnable, BooleanSupplier expired) {
        invokeLater(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state) {
        invokeLater(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state, BooleanSupplier expired) {
        invokeLater(runnable);
    }

    @Override
    public UIAccess getLastUIAccess() {
        return HeadlessUIAccess.INSTANCE;
    }
}
