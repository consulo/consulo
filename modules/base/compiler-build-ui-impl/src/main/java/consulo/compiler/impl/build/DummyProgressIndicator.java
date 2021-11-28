/*
 * Copyright 2013-2021 consulo.io
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
package consulo.compiler.impl.build;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 28/11/2021
 */
public class DummyProgressIndicator implements ProgressIndicatorEx {
  @Override
  public void addStateDelegate(@Nonnull ProgressIndicatorEx delegate) {

  }

  @Override
  public void finish(@Nonnull TaskInfo task) {

  }

  @Override
  public boolean isFinished(@Nonnull TaskInfo task) {
    return false;
  }

  @Override
  public boolean wasStarted() {
    return false;
  }

  @Override
  public void processFinish() {

  }

  @Override
  public void initStateFrom(@Nonnull ProgressIndicator indicator) {

  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void cancel() {

  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public void setTextValue(@Nonnull LocalizeValue textValue) {

  }

  @Nonnull
  @Override
  public LocalizeValue getTextValue() {
    return LocalizeValue.of();
  }

  @Override
  public void setText2Value(@Nonnull LocalizeValue text) {

  }

  @Nonnull
  @Override
  public LocalizeValue getText2Value() {
    return LocalizeValue.of();
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
  public boolean isModal() {
    return false;
  }

  @Nonnull
  @Override
  public ModalityState getModalityState() {
    return ModalityState.defaultModalityState();
  }

  @Override
  public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {

  }

  @Override
  public boolean isIndeterminate() {
    return false;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {

  }

  @Override
  public void checkCanceled() throws ProcessCanceledException {

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
