/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.wizard;

import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public interface WizardStep<CONTEXT> {

  @Nonnull
  @RequiredUIAccess
  Component getComponent(@Nonnull CONTEXT context, @Nonnull Disposable uiDisposable);

  @Nullable
  default Component getPreferredFocusedComponent() {
    return null;
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Desktop UI version")
  @RequiredUIAccess
  default java.awt.Component getSwingComponent(@Nonnull CONTEXT context, @Nonnull Disposable uiDisposable) {
    return TargetAWT.to(getComponent(context, uiDisposable));
  }

  @Nullable
  @Deprecated
  @DeprecationInfo("Desktop UI version")
  default java.awt.Component getSwingPreferredFocusedComponent() {
    return TargetAWT.to(getPreferredFocusedComponent());
  }

  default void onStepEnter(@Nonnull CONTEXT context) {
  }

  default void onStepLeave(@Nonnull CONTEXT context) {
  }

  default void validateStep(@Nonnull CONTEXT context) throws WizardStepValidationException {
  }

  default boolean isVisible(@Nonnull CONTEXT context) {
    return true;
  }
}
