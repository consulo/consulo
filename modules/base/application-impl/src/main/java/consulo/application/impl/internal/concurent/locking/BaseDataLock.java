/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.impl.internal.concurent.locking;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.concurrent.DataLock;
import consulo.logging.Logger;
import consulo.util.concurrent.internal.ThreadAssertion;
import consulo.util.lang.function.ThrowableSupplier;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-11-14
 */
public abstract class BaseDataLock implements DataLock {
  protected static final Logger LOG = Logger.getInstance(BaseDataLock.class);

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {
    ThreadAssertion.assertTrue(!isWriteAccessAllowed(), "Write access is allowed inside write-action");
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    ThreadAssertion.assertTrue(!isReadAccessAllowed(), "Read access is only under read action");
  }

  public abstract boolean isWriteActionInProgress();

  public abstract boolean tryReadSync(Runnable runnable);

  public abstract <T, E extends Throwable> T runWriteActionUnsafe(@Nonnull ThrowableSupplier<T, E> computation) throws E;
}
