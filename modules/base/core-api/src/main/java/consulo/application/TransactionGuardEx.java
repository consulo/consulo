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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public abstract class TransactionGuardEx extends TransactionGuard {
  public void enteredModality(@Nonnull ModalityState modality) {
  }

  public void assertWriteActionAllowed() {
  }

  public void performUserActivity(Runnable activity) {
    activity.run();
  }

  @Nonnull
  public AccessToken startActivity(boolean userActivity) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  public boolean isWriteSafeModality(ModalityState state) {
    return true;
  }

  @Nullable
  public TransactionId getModalityTransaction(@Nonnull ModalityState modalityState) {
    return null;
  }
}
