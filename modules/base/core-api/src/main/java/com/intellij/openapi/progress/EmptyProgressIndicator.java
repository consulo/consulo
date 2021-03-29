/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.openapi.progress;

import com.intellij.openapi.application.ModalityState;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyProgressIndicator implements StandardProgressIndicator {

  private static final Logger LOG = Logger.getInstance(EmptyProgressIndicator.class);

  @Nonnull
  private final ModalityState myModalityState;

  private volatile boolean myIsRunning;
  private volatile boolean myIsCanceled;

  public EmptyProgressIndicator() {
    this(ModalityState.defaultModalityState());
  }

  public EmptyProgressIndicator(@Nonnull ModalityState modalityState) {
    myModalityState = modalityState;
  }

  @Override
  public void start() {
    myIsRunning = true;
    myIsCanceled = false;
  }

  @Override
  public void stop() {
    myIsRunning = false;
  }

  @Override
  public boolean isRunning() {
    return myIsRunning;
  }

  @Override
  public final void cancel() {
    myIsCanceled = true;
    ProgressManager.canceled(this);
  }

  @Override
  public final boolean isCanceled() {
    return myIsCanceled;
  }

  @Override
  public final void checkCanceled() {
    if (myIsCanceled) {
      throw new ProcessCanceledException();
    }
  }

  @Override
  public void setTextValue(LocalizeValue text) {
  }

  @Override
  public LocalizeValue getTextValue() {
    return LocalizeValue.empty();
  }

  @Override
  public void setText2Value(LocalizeValue text) {
  }

  @Override
  public LocalizeValue getText2Value() {
    return LocalizeValue.empty();
  }

  @Override
  public double getFraction() {
    return 1;
  }

  @Override
  public void setFraction(double fraction) {
  }

  @Override
  public void pushState() {
  }

  @Override
  public void popState() {
  }

  @Override
  public void startNonCancelableSection() {
  }

  @Override
  public void finishNonCancelableSection() {
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  @Nonnull
  public ModalityState getModalityState() {
    return myModalityState;
  }

  @Override
  public void setModalityProgress(ProgressIndicator modalityProgress) {
  }

  @Override
  public boolean isIndeterminate() {
    return false;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
  }

  @Override
  public boolean isPopupWasShown() {
    return false;
  }

  @Override
  public boolean isShowing() {
    return false;
  }

  @Nonnull
  public static ProgressIndicator notNullize(@Nullable ProgressIndicator indicator) {
    if (indicator != null) {
      return indicator;
    }
    LOG.info("No progress indicator");
    return new EmptyProgressIndicator();
  }
}
