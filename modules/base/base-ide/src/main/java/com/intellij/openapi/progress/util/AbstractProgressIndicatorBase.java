// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.progress.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.impl.ModalityStateEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.mac.foundation.MacUtil;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.Stack;
import consulo.application.TransactionGuardEx;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.primitive.doubles.DoubleList;
import consulo.util.collection.primitive.doubles.DoubleLists;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AbstractProgressIndicatorBase extends UserDataHolderBase implements ProgressIndicator {
  private static final Logger LOG = Logger.getInstance(AbstractProgressIndicatorBase.class);

  private volatile LocalizeValue myText = LocalizeValue.empty();
  private volatile double myFraction;
  private volatile LocalizeValue myText2 = LocalizeValue.empty();

  private volatile boolean myCanceled;
  private volatile boolean myRunning;
  private volatile boolean myFinished;

  private volatile boolean myIndeterminate = Registry.is("ide.progress.indeterminate.by.default", true);
  private volatile MacUtil.Activity myMacActivity;
  private volatile boolean myShouldStartActivity = true;

  private Stack<LocalizeValue> myTextStack; // guarded by this
  private DoubleList myFractionStack; // guarded by this
  private Stack<LocalizeValue> myText2Stack; // guarded by this

  private ProgressIndicator myModalityProgress;
  private volatile ModalityState myModalityState = ModalityState.NON_MODAL;
  private volatile int myNonCancelableSectionCount;
  private final Object lock = ObjectUtil.sentinel("APIB lock");

  @Override
  public void start() {
    synchronized (getLock()) {
      LOG.assertTrue(!isRunning(), "Attempt to start ProgressIndicator which is already running");
      if (myFinished) {
        if (myCanceled && !isReuseable()) {
          if (ourReportedReuseExceptions.add(getClass())) {
            LOG.error("Attempt to start ProgressIndicator which is cancelled and already stopped:" + this + "," + getClass());
          }
        }
        myCanceled = false;
        myFinished = false;
      }

      myText = LocalizeValue.empty();
      myFraction = 0;
      myText2 = LocalizeValue.empty();
      startSystemActivity();
      myRunning = true;
    }
  }

  private static final Set<Class> ourReportedReuseExceptions = ConcurrentHashMap.newKeySet();

  protected boolean isReuseable() {
    return false;
  }

  @Override
  public void stop() {
    synchronized (getLock()) {
      LOG.assertTrue(myRunning, "stop() should be called only if start() called before");
      myRunning = false;
      myFinished = true;
      stopSystemActivity();
    }
  }

  private void startSystemActivity() {
    myMacActivity = myShouldStartActivity ? MacUtil.wakeUpNeo(this) : null;
  }

  void stopSystemActivity() {
    MacUtil.Activity macActivity = myMacActivity;
    if (macActivity != null) {
      macActivity.matrixHasYou();
      myMacActivity = null;
    }
  }

  @Override
  public boolean isRunning() {
    return myRunning;
  }

  @Override
  public void cancel() {
    myCanceled = true;
    stopSystemActivity();
    if (ApplicationManager.getApplication() != null) {
      ProgressManager.canceled(this);
    }
  }

  @Override
  public boolean isCanceled() {
    return myCanceled;
  }

  @Override
  public void checkCanceled() {
    throwIfCanceled();
    if (CoreProgressManager.runCheckCanceledHooks(this)) {
      throwIfCanceled();
    }
  }

  private void throwIfCanceled() {
    if (isCanceled() && isCancelable()) {
      Throwable trace = getCancellationTrace();
      throw trace instanceof ProcessCanceledException ? (ProcessCanceledException)trace : new ProcessCanceledException(trace);
    }
  }

  @Nullable
  protected Throwable getCancellationTrace() {
    if (this instanceof Disposable) {
      return Disposer.getDisposalTrace((Disposable)this);
    }
    return null;
  }

  @Override
  public void setTextValue(final LocalizeValue text) {
    myText = text;
  }

  @Override
  public LocalizeValue getTextValue() {
    return myText;
  }

  @Override
  public void setText2Value(final LocalizeValue text) {
    myText2 = text;
  }

  @Override
  public LocalizeValue getText2Value() {
    return myText2;
  }

  @Override
  public double getFraction() {
    return myFraction;
  }

  @Override
  public void setFraction(final double fraction) {
    if (isIndeterminate()) {
      StackTraceElement[] trace = new Throwable().getStackTrace();
      Optional<StackTraceElement> first = Arrays.stream(trace).filter(element -> !element.getClassName().startsWith("com.intellij.openapi.progress.util")).findFirst();
      String message = "This progress indicator is indeterminate, this may lead to visual inconsistency. " + "Please call setIndeterminate(false) before you start progress.";
      if (first.isPresent()) {
        message += "\n" + first.get();
      }
      LOG.warn(message);
      setIndeterminate(false);
    }
    myFraction = fraction;
  }

  @Override
  public void pushState() {
    synchronized (getLock()) {
      getTextStack().push(myText);
      getFractionStack().add(myFraction);
      getText2Stack().push(myText2);
    }
  }

  @Override
  public void popState() {
    synchronized (getLock()) {
      LOG.assertTrue(!myTextStack.isEmpty());
      LocalizeValue oldText = myTextStack.pop();
      LocalizeValue oldText2 = myText2Stack.pop();
      setTextValue(oldText);
      setText2Value(oldText2);

      double oldFraction = myFractionStack.removeByIndex(myFractionStack.size() - 1);
      if (!isIndeterminate()) {
        setFraction(oldFraction);
      }
    }
  }

  @Override
  public void startNonCancelableSection() {
    myNonCancelableSectionCount++;
  }

  @Override
  public void finishNonCancelableSection() {
    myNonCancelableSectionCount--;
  }

  protected boolean isCancelable() {
    return myNonCancelableSectionCount == 0 && !ProgressManager.getInstance().isInNonCancelableSection();
  }

  @Override
  public final boolean isModal() {
    return myModalityProgress != null;
  }

  final boolean isModalEntity() {
    return myModalityProgress == this;
  }

  @Override
  @Nonnull
  public ModalityState getModalityState() {
    return myModalityState;
  }

  @Override
  public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {
    LOG.assertTrue(!isRunning());
    myModalityProgress = modalityProgress;
    setModalityState(modalityProgress);
  }

  private void setModalityState(@Nullable ProgressIndicator modalityProgress) {
    ModalityState modalityState = ModalityState.defaultModalityState();

    if (modalityProgress != null) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      modalityState = ((ModalityStateEx)modalityState).appendProgress(modalityProgress);
      ((TransactionGuardEx)TransactionGuard.getInstance()).enteredModality(modalityState);
    }

    myModalityState = modalityState;
  }

  @Override
  public boolean isIndeterminate() {
    return myIndeterminate;
  }

  @Override
  public void setIndeterminate(final boolean indeterminate) {
    myIndeterminate = indeterminate;
  }

  @Override
  public String toString() {
    return "ProgressIndicator " + System.identityHashCode(this) + ": running=" + isRunning() + "; canceled=" + isCanceled();
  }

  @Override
  public boolean isPopupWasShown() {
    return true;
  }

  @Override
  public boolean isShowing() {
    return isModal();
  }

  public void initStateFrom(@Nonnull final ProgressIndicator indicator) {
    synchronized (getLock()) {
      myRunning = indicator.isRunning();
      myCanceled = indicator.isCanceled();
      myFraction = indicator.getFraction();
      myIndeterminate = indicator.isIndeterminate();
      myText = indicator.getTextValue();
      myText2 = indicator.getText2Value();

      myFraction = indicator.getFraction();

      if (indicator instanceof AbstractProgressIndicatorBase) {
        AbstractProgressIndicatorBase stacked = (AbstractProgressIndicatorBase)indicator;

        myTextStack = stacked.myTextStack == null ? null : new Stack<>(stacked.getTextStack());

        myText2Stack = stacked.myText2Stack == null ? null : new Stack<>(stacked.getText2Stack());

        myFractionStack = stacked.myFractionStack == null ? null : DoubleLists.newArrayList(stacked.getFractionStack().toArray());
      }
      dontStartActivity();
    }
  }

  protected void dontStartActivity() {
    myShouldStartActivity = false;
  }

  @Nonnull
  private Stack<LocalizeValue> getTextStack() {
    Stack<LocalizeValue> stack = myTextStack;
    if (stack == null) myTextStack = stack = new Stack<>(2);
    return stack;
  }

  @Nonnull
  private DoubleList getFractionStack() {
    DoubleList stack = myFractionStack;
    if (stack == null) myFractionStack = stack = DoubleLists.newArrayList(2);
    return stack;
  }

  @Nonnull
  private Stack<LocalizeValue> getText2Stack() {
    Stack<LocalizeValue> stack = myText2Stack;
    if (stack == null) myText2Stack = stack = new Stack<>(2);
    return stack;
  }

  @Nonnull
  protected Object getLock() {
    return lock;
  }
}
