// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.cache;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.util.lang.ObjectUtil;
import javax.annotation.Nonnull;

import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.Nullable;
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

  private final static ExecutorService ourExecutorService = AppExecutorUtil.createBoundedApplicationPoolExecutor("CodeStyleCachedValueProvider", MAX_COMPUTATION_THREADS);

  CodeStyleCachedValueProvider(@Nonnull PsiFile file) {
    myFileRef = new WeakReference<>(file);
    myComputation = new AsyncComputation();
  }

  boolean isExpired() {
    return myFileRef.get() == null || myComputation.isExpired();
  }

  CodeStyleSettings tryGetSettings() {
    try {
      final PsiFile file = getReferencedPsi();
      if (myComputationLock.tryLock()) {
        try {
          return CachedValuesManager.getCachedValue(file, this);
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
    if (settings instanceof TransientCodeStyleSettings) {
      dependencies.addAll(((TransientCodeStyleSettings)settings).getDependencies());
    }
    else {
      dependencies.add(settings.getModificationTracker());
    }
    dependencies.add(computation.getTracker());
    return ArrayUtil.toObjectArray(dependencies);
  }

  private static void logCached(@Nonnull PsiFile file, @Nonnull CodeStyleSettings settings) {
    LOG.debug(String.format("File: %s (%s), cached: %s, tracker: %d", file.getName(), Integer.toHexString(file.hashCode()), settings, settings.getModificationTracker().getModificationCount()));
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
        myPromise = ReadAction.nonBlocking(() -> computeSettings()).expireWith(myProject).expireWhen(() -> myFileRef.get() == null)
                .finishOnUiThread(ModalityState.NON_MODAL, val -> notifyCachedValueComputed()).submit(ourExecutorService);
      }
      else {
        ReadAction.run((() -> computeSettings()));
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
      final Application application = ApplicationManager.getApplication();
      return !application.isUnitTestMode() && !application.isHeadlessEnvironment() && application.isDispatchThread();
    }

    private void notifyOnEdt() {
      final Application application = ApplicationManager.getApplication();
      if (application.isDispatchThread()) {
        notifyCachedValueComputed();
      }
      else {
        application.invokeLater(() -> notifyCachedValueComputed(), ModalityState.any());
      }
    }

    private void computeSettings() {
      final PsiFile file = myFileRef.get();
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
          final CodeStyleSettingsManager settingsManager = CodeStyleSettingsManager.getInstance(myProject);
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
  public boolean equals(Object obj) {
    return obj instanceof CodeStyleCachedValueProvider && Objects.equals(this.myFileRef.get(), ((CodeStyleCachedValueProvider)obj).myFileRef.get());
  }

  static class OutdatedFileReferenceException extends RuntimeException {
    OutdatedFileReferenceException() {
      super("Outdated file reference used to obtain settings");
    }
  }
}
