/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application.impl.internal;

import consulo.application.Application;
import consulo.application.TransactionId;
import consulo.component.ProcessCanceledException;
import consulo.application.internal.TransactionGuardEx;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public class UnifiedTransactionGuardImpl extends TransactionGuardEx {
  @Override
  public void assertWriteSafeContext(@Nonnull ModalityState modality) {

  }

  @Override
  public void submitTransactionLater(@Nonnull Disposable parentDisposable, @Nonnull Runnable runnable) {
    runnable.run();
  }

  @Override
  public void submitTransactionAndWait(@Nonnull Runnable runnable) throws ProcessCanceledException {
    runnable.run();
  }

  @Override
  public void submitTransaction(@Nonnull Disposable parentDisposable, @Nullable TransactionId expectedContext, @Nonnull Runnable runnable) {
    Application.get().getLastUIAccess().giveIfNeed(runnable);
  }

  @Override
  public TransactionId getContextTransaction() {
    return null;
  }
}
