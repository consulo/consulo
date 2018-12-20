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
package consulo.application;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionId;
import com.intellij.openapi.progress.ProcessCanceledException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-12-20
 */
public class DummyTransactionGuard extends TransactionGuardEx {
  @Override
  public void assertWriteSafeContext(@Nonnull ModalityState modality) {

  }

  @Override
  public void submitTransactionLater(@Nonnull Disposable parentDisposable, @Nonnull Runnable transaction) {
    transaction.run();
  }

  @Override
  public void submitTransactionAndWait(@Nonnull Runnable transaction) throws ProcessCanceledException {
    transaction.run();
  }

  @Override
  public void submitTransaction(@Nonnull Disposable parentDisposable, @Nullable TransactionId expectedContext, @Nonnull Runnable transaction) {
    transaction.run();
  }

  @Override
  public TransactionId getContextTransaction() {
    return null;
  }
}
