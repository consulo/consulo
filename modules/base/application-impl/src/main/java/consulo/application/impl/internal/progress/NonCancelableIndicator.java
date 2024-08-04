/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.application.impl.internal.progress;

import consulo.application.internal.NonCancelableSection;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.StandardProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;

class NonCancelableIndicator implements NonCancelableSection, StandardProgressIndicator {
  static final NonCancelableIndicator INSTANCE = new NonCancelableIndicator() {
    @Override
    public int hashCode() {
      return 0;
    }
  };

  protected NonCancelableIndicator() {
  }

  @Override
  public void done() {
    ProgressIndicator currentIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (currentIndicator != this) {
      throw new AssertionError("Trying do .done() NonCancelableSection, which is already done");
    }
  }

  @Override
  public final void checkCanceled() {
    CoreProgressManager.runCheckCanceledHooks(this);
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public final void cancel() {

  }

  @Override
  public final boolean isCanceled() {
    return false;
  }

  @Override
  public void setTextValue(@Nonnull LocalizeValue text) {

  }

  @Nonnull
  @Override
  public LocalizeValue getTextValue() {
    return LocalizeValue.empty();
  }

  @Override
  public void setText2Value(LocalizeValue text) {

  }

  @Nonnull
  @Override
  public LocalizeValue getText2Value() {
    return LocalizeValue.empty();
  }

  @Override
  public double getFraction() {
    return 0;
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

  @Nonnull
  @Override
  public ModalityState getModalityState() {
    return ModalityState.nonModal();
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
}
