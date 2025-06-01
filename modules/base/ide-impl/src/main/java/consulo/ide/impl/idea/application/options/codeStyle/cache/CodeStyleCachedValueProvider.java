// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.application.options.codeStyle.cache;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.util.SimpleModificationTracker;
import consulo.ide.impl.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import consulo.ide.impl.psi.codeStyle.modifier.TransientCodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CodeStyleCachedValueProvider implements CachedValueProvider<CodeStyleSettings> {
    private final static Logger LOG = Logger.getInstance(CodeStyleCachedValueProvider.class);

    private final static int MAX_COMPUTATION_THREADS = 10;

    private final
    @Nonnull
    WeakReference<PsiFile> myFileRef;
    private final
    @Nonnull
    AsyncComputation myComputation;
    private final
    @Nonnull
    Lock myComputationLock = new ReentrantLock();

    private final static ExecutorService ourExecutorService =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("CodeStyleCachedValueProvider", MAX_COMPUTATION_THREADS);

    CodeStyleCachedValueProvider(@Nonnull PsiFile file) {
        myFileRef = new WeakReference<>(file);
        myComputation = new AsyncComputation();
    }

    boolean isExpired() {
        return myFileRef.get() == null || myComputation.isExpired();
    }

    CodeStyleSettings tryGetSettings() {
        try {
            PsiFile file = getReferencedPsi();
            if (myComputationLock.tryLock()) {
                try {
                    return LanguageCachedValueUtil.getCachedValue(file, this);
                }
                finally {
                    myComputationLock.unlock();
                }
            }
            else {
                return null;
            }
        }
        catch (OutdatedFileReferenceException e) {
            LOG.error(e);
            return null;
        }
    }

    void scheduleWhenComputed(@Nonnull Runnable runnable) {
        myComputation.schedule(runnable);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public Result<CodeStyleSettings> compute() {
        CodeStyleSettings settings = myComputation.getCurrResult();
        if (settings != null) {
            logCached(getReferencedPsi(), settings);
            return new Result<>(settings, getDependencies(settings, myComputation));
        }
        return null;
    }

    public void cancelComputation() {
        myComputation.cancel();
    }

    @Nonnull
    Object[] getDependencies(@Nonnull CodeStyleSettings settings, @Nonnull AsyncComputation computation) {
        List<Object> dependencies = new ArrayList<>();
        if (settings instanceof TransientCodeStyleSettings codeStyleSettings) {
            dependencies.addAll(codeStyleSettings.getDependencies());
        }
        else {
            dependencies.add(settings.getModificationTracker());
        }
        dependencies.add(computation.mySettingsManager.getModificationTracker());
        dependencies.add(computation.getTracker());
        return ArrayUtil.toObjectArray(dependencies);
    }

    @RequiredReadAction
    private static void logCached(@Nonnull PsiFile file, @Nonnull CodeStyleSettings settings) {
        LOG.debug(String.format(
            "File: %s (%s), cached: %s, tracker: %d",
            file.getName(),
            Integer.toHexString(file.hashCode()),
            settings,
            settings.getModificationTracker().getModificationCount()
        ));
    }

    /**
     * Always contains some result which can be obtained by {@code getCurrResult()} method. Listeners are notified after
     * the computation is finished and {@code getCurrResult()} contains a stable computed value.
     */
    private final class AsyncComputation {
        private final AtomicBoolean myIsActive = new AtomicBoolean();
        private volatile CodeStyleSettings myCurrResult;
        private final
        @Nonnull
        CodeStyleSettingsManager mySettingsManager;
        private final SimpleModificationTracker myTracker = new SimpleModificationTracker();
        private final Project myProject;
        private CancellablePromise<Void> myPromise;
        private final List<Runnable> myScheduledRunnables = new ArrayList<>();

        private AsyncComputation() {
            myProject = getReferencedPsi().getProject();
            mySettingsManager = CodeStyleSettingsManager.getInstance(myProject);
            //noinspection deprecation
            myCurrResult = mySettingsManager.getCurrentSettings();
        }

        private void start() {
            if (isRunOnBackground()) {
                myPromise = ReadAction.nonBlocking(this::computeSettings)
                    .expireWith(myProject)
                    .expireWhen(() -> myFileRef.get() == null)
                    .finishOnUiThread(Application::getNoneModalityState, val -> notifyCachedValueComputed())
                    .submit(ourExecutorService);
            }
            else {
                ReadAction.run(this::computeSettings);
                notifyOnEdt();
            }
        }

        public void cancel() {
            if (myPromise != null && !myPromise.isDone()) {
                myPromise.cancel();
            }
            myCurrResult = null;
        }

        public boolean isExpired() {
            return myCurrResult == null;
        }

        private void schedule(@Nonnull Runnable runnable) {
            if (myIsActive.get()) {
                myScheduledRunnables.add(runnable);
            }
            else {
                runnable.run();
            }
        }

        private boolean isRunOnBackground() {
            Application application = Application.get();
            return !application.isUnitTestMode() && !application.isHeadlessEnvironment() && application.isDispatchThread();
        }

        private void notifyOnEdt() {
            Application application = Application.get();
            if (application.isDispatchThread()) {
                notifyCachedValueComputed();
            }
            else {
                application.invokeLater(this::notifyCachedValueComputed, application.getAnyModalityState());
            }
        }

        @RequiredReadAction
        private void computeSettings() {
            PsiFile file = myFileRef.get();
            if (file == null) {
                LOG.warn("PSI file has expired, cancelling computation");
                cancel();
                return;
            }
            try {
                myComputationLock.lock();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Computation started for " + file.getName());
                }
                @SuppressWarnings("deprecation") CodeStyleSettings currSettings = mySettingsManager.getCurrentSettings();
                if (currSettings != mySettingsManager.getTemporarySettings()) {
                    TransientCodeStyleSettings modifiableSettings = new TransientCodeStyleSettings(file, currSettings);
                    modifiableSettings.applyIndentOptionsFromProviders(file);
                    for (CodeStyleSettingsModifier modifier : CodeStyleSettingsModifier.EP_NAME.getExtensionList()) {
                        if (modifier.modifySettings(modifiableSettings, file)) {
                            LOG.debug("Modifier: " + modifier.getClass().getName());
                            modifiableSettings.setModifier(modifier);
                            currSettings = modifiableSettings;
                            break;
                        }
                    }
                }
                myCurrResult = currSettings;
                myTracker.incModificationCount();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Computation ended for " + file.getName());
                }
            }
            finally {
                myComputationLock.unlock();
            }
        }

        @Nullable
        public CodeStyleSettings getCurrResult() {
            if (myIsActive.compareAndSet(false, true)) {
                start();
            }
            return myCurrResult;
        }

        private SimpleModificationTracker getTracker() {
            return myTracker;
        }

        void reset() {
            myScheduledRunnables.clear();
            myIsActive.set(false);
        }

        private void notifyCachedValueComputed() {
            for (Runnable runnable : myScheduledRunnables) {
                runnable.run();
            }
            if (!myProject.isDisposed()) {
                ObjectUtil.consumeIfNotNull(myFileRef.get(), file -> {
                    CodeStyleSettingsManager settingsManager = CodeStyleSettingsManager.getInstance(myProject);
                    settingsManager.fireCodeStyleSettingsChanged(file);
                });
            }
            myComputation.reset();
        }
    }

    @Nonnull
    private PsiFile getReferencedPsi() {
        PsiFile file = myFileRef.get();
        if (file == null) {
            throw new OutdatedFileReferenceException();
        }
        return file;
    }

    //
    // Check provider equivalence by file ref. Other fields make no sense since AsyncComputation is a stateful object
    // whose state (active=true->false) changes over time due to long computation.
    //
    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals(Object obj) {
        return obj instanceof CodeStyleCachedValueProvider valueProvider
            && Objects.equals(this.myFileRef.get(), valueProvider.myFileRef.get());
    }

    static class OutdatedFileReferenceException extends RuntimeException {
        OutdatedFileReferenceException() {
            super("Outdated file reference used to obtain settings");
        }
    }
}
