// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.index.impl.internal;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

// This class is thread safe
final class IndexingProgressReporter {
    private static final Logger LOG = Logger.getInstance(IndexingProgressReporter.class);

    private static final long PARK_NANOS = 50_000_000;

    private volatile int mySubTasksCount = 0;
    private final ObservableValue<@Nullable LocalizeValue> myOperationName = ObservableValue.of(null);
    private final ObservableValue<Integer> mySubTasksFinished = ObservableValue.of(0);
    private final ObservableValue<List<LocalizeValue>> mySubTaskTexts = ObservableValue.of(List.of());

    int getSubTasksCount() {
        return mySubTasksCount;
    }

    ObservableValue<@Nullable LocalizeValue> getOperationName() {
        return myOperationName;
    }

    ObservableValue<Integer> getSubTasksFinished() {
        return mySubTasksFinished;
    }

    ObservableValue<List<LocalizeValue>> getSubTaskTexts() {
        return mySubTaskTexts;
    }

    void setSubTasksCount(int value) {
        LOG.assertTrue(mySubTasksCount == 0, "subTasksCount can be set only once. Previous value: " + mySubTasksCount);
        mySubTasksCount = value;
    }

    void setText(LocalizeValue value) {
        myOperationName.set(value);
    }

    IndexingSubTaskProgressReporter getSubTaskReporter() {
        return new IndexingSubTaskProgressReporter();
    }

    static Runnable launchIndexingProgressUIReporter(
        Project project,
        ObservableValue<Boolean> shouldShowProgress,
        IndexingProgressReporter progressReporter,
        LocalizeValue progressTitle,
        ObservableValue<@Nullable LocalizeValue> pauseReason
    ) {
        IndexingProgressUIReporter uiReporter =
            new IndexingProgressUIReporter(project, shouldShowProgress, progressReporter, progressTitle, pauseReason);
        uiReporter.start();
        return uiReporter::stop;
    }

    private static final class IndexingProgressUIReporter {
        private static final AtomicBoolean ourStatusBarDegradeReported = new AtomicBoolean();

        private final Project myProject;
        private final ObservableValue<Boolean> myShouldShowProgress;
        private final IndexingProgressReporter myProgressReporter;
        private final LocalizeValue myProgressTitle;
        private final ObservableValue<@Nullable LocalizeValue> myPauseReason;

        private final AtomicBoolean myStopped = new AtomicBoolean();
        private final AtomicBoolean myShowing = new AtomicBoolean();
        private volatile @Nullable Runnable myShouldShowProgressSubscription;

        IndexingProgressUIReporter(
            Project project,
            ObservableValue<Boolean> shouldShowProgress,
            IndexingProgressReporter progressReporter,
            LocalizeValue progressTitle,
            ObservableValue<@Nullable LocalizeValue> pauseReason
        ) {
            myProject = project;
            myShouldShowProgress = shouldShowProgress;
            myProgressReporter = progressReporter;
            myProgressTitle = progressTitle;
            myPauseReason = pauseReason;
        }

        void start() {
            myShouldShowProgressSubscription = myShouldShowProgress.addListener(value -> showIfNeeded());
            showIfNeeded();
        }

        void stop() {
            myStopped.set(true);
            Runnable subscription = myShouldShowProgressSubscription;
            if (subscription != null) {
                myShouldShowProgressSubscription = null;
                subscription.run();
            }
        }

        private void showIfNeeded() {
            if (myStopped.get() || myProject.isDisposed() || !myShouldShowProgress.get() || !myShowing.compareAndSet(false, true)) {
                return;
            }
            if (ourStatusBarDegradeReported.compareAndSet(false, true)) {
                LOG.debug("Background progress without status bar presence is not supported, "
                    + "scanning progress is shown as a regular background task");
            }
            myProject.getApplication().executeOnPooledThread(() -> {
                if (myStopped.get() || myProject.isDisposed()) {
                    myShowing.set(false);
                    return;
                }
                new Task.Backgroundable(myProject, myProgressTitle, false) {
                    @Override
                    public void run(ProgressIndicator indicator) {
                        report(indicator);
                    }
                }.queue();
            });
        }

        private void report(ProgressIndicator indicator) {
            List<Runnable> subscriptions = new ArrayList<>();
            try {
                Runnable updateText = () -> {
                    LocalizeValue paused = myPauseReason.get();
                    LocalizeValue operation = myProgressReporter.getOperationName().get();
                    if (paused != null) {
                        indicator.setText(ProjectLocalize.dumbServiceIndexingPausedDueTo(paused));
                    }
                    else {
                        indicator.setText(operation != null ? operation : myProgressTitle);
                    }
                };
                Runnable updateDetails = () -> {
                    List<LocalizeValue> texts = myProgressReporter.getSubTaskTexts().get();
                    indicator.setText2(texts.isEmpty() ? LocalizeValue.empty() : texts.get(0));
                };
                Runnable updateFraction = () -> {
                    int subTasksCount = myProgressReporter.getSubTasksCount();
                    if (subTasksCount > 0) {
                        double fraction = myProgressReporter.getSubTasksFinished().get().doubleValue() / subTasksCount;
                        double newValue = Math.min(1.0, Math.max(0.0, fraction));
                        indicator.setIndeterminate(false);
                        indicator.setFraction(newValue);
                    }
                    else {
                        indicator.setIndeterminate(true);
                    }
                };

                subscriptions.add(myPauseReason.addListener(value -> updateText.run()));
                subscriptions.add(myProgressReporter.getOperationName().addListener(value -> updateText.run()));
                subscriptions.add(myProgressReporter.getSubTaskTexts().addListener(value -> updateDetails.run()));
                subscriptions.add(myProgressReporter.getSubTasksFinished().addListener(value -> updateFraction.run()));

                updateText.run();
                updateDetails.run();
                updateFraction.run();

                while (!myStopped.get() && myShouldShowProgress.get() && !myProject.isDisposed()) {
                    LockSupport.parkNanos(PARK_NANOS);
                }
            }
            finally {
                for (Runnable subscription : subscriptions) {
                    subscription.run();
                }
                myShowing.set(false);
                showIfNeeded();
            }
        }
    }

    // This class is not thread safe
    final class IndexingSubTaskProgressReporter implements AutoCloseable {
        private @Nullable LocalizeValue myOldText = null;

        void setText(LocalizeValue value) {
            // First add, then remove. To avoid blinking if old text is the only text in the list
            // We insert to the beginning to make sure that the text is rendered immediately. Otherwise, there will be an impression that
            // indexing is slow, if the text does not change often enough.
            mySubTaskTexts.update(texts -> {
                List<LocalizeValue> copy = new ArrayList<>(texts.size() + 1);
                copy.add(value);
                copy.addAll(texts);
                return List.copyOf(copy);
            });
            LocalizeValue oldText = myOldText;
            if (oldText != null) {
                mySubTaskTexts.update(texts -> removing(texts, oldText /* this class should be used from single thread */));
            }
            myOldText = value;
        }

        @Override
        public void close() {
            mySubTasksFinished.update(finished -> finished + 1);
            LocalizeValue oldText = myOldText;
            if (oldText != null) {
                mySubTaskTexts.update(texts -> removing(texts, oldText /* this class should be used from single thread */));
                myOldText = null;
            }
        }

        private static List<LocalizeValue> removing(List<LocalizeValue> texts, LocalizeValue text) {
            List<LocalizeValue> copy = new ArrayList<>(texts);
            copy.remove(text);
            return List.copyOf(copy);
        }
    }

    interface CheckPauseOnlyProgressIndicator {
        boolean isPaused();

        void suspendIfPaused();

        void onPausedStateChanged(Consumer<Boolean> action);
    }

    static final class CheckPauseOnlyProgressIndicatorImpl implements CheckPauseOnlyProgressIndicator, Disposable {
        private final ObservableValue<List<LocalizeValue>> myPauseReason;
        private final ObservableValue<@Nullable LocalizeValue> myFirstPauseReason;
        private final ObservableValue<Boolean> myPaused;
        private final ProgressIndicatorBase myProgressIndicator = new ProgressIndicatorBase();
        private final List<Runnable> mySubscriptions = new CopyOnWriteArrayList<>();

        CheckPauseOnlyProgressIndicatorImpl(ObservableValue<List<LocalizeValue>> pauseReason) {
            myPauseReason = pauseReason;
            myFirstPauseReason = ObservableValue.of(firstOrNull(pauseReason.get()));
            myPaused = ObservableValue.of(myFirstPauseReason.get() != null);
            mySubscriptions.add(pauseReason.addListener(reasons -> myFirstPauseReason.set(firstOrNull(reasons))));
            mySubscriptions.add(myFirstPauseReason.addListener(reason -> myPaused.set(reason != null)));
        }

        private static @Nullable LocalizeValue firstOrNull(List<LocalizeValue> reasons) {
            return reasons.isEmpty() ? null : reasons.get(0);
        }

        ObservableValue<@Nullable LocalizeValue> getPauseReason() {
            return myFirstPauseReason;
        }

        ProgressIndicator getProgressIndicator() {
            return myProgressIndicator;
        }

        @Override
        public void onPausedStateChanged(Consumer<Boolean> action) {
            action.accept(myPaused.get());
            mySubscriptions.add(myPaused.addListener(action::accept));
        }

        @Override
        public boolean isPaused() {
            return !myPauseReason.get().isEmpty();
        }

        @Override
        public void suspendIfPaused() {
            while (isPaused()) {
                ProgressManager.checkCanceled();
                LockSupport.parkNanos(PARK_NANOS);
            }
        }

        @Override
        public void dispose() {
            for (Runnable subscription : mySubscriptions) {
                subscription.run();
            }
            mySubscriptions.clear();
        }
    }
}
