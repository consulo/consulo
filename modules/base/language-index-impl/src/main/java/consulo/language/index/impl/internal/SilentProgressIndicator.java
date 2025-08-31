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
package consulo.language.index.impl.internal;

import consulo.application.progress.DelegatingProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim.Mossienko on 11/30/2015.
 */
class SilentProgressIndicator extends DelegatingProgressIndicator {
  // suppress verbose messages

  private SilentProgressIndicator(ProgressIndicator indicator) {
    super(indicator);
  }

  @Nullable
  static SilentProgressIndicator create() {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    return indicator != null ? new SilentProgressIndicator(indicator) : null;
  }

  @Override
  public void setTextValue(LocalizeValue text) {
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
}
