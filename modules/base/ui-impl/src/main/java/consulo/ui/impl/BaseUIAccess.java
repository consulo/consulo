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
package consulo.ui.impl;

import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.UIAccessScheduler;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 14/09/2023
 */
public abstract class BaseUIAccess implements UIAccess {
  protected static final Logger LOG = Logger.getInstance(UIAccess.class);

  @Nonnull
  protected Runnable wrapRunnable(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      }
      catch (Throwable e) {
        if (e instanceof ControlFlowException) {
          return;
        }

        LOG.error(e);

        ExceptionUtil.rethrow(e);
      }
    };
  }

  protected SingleUIAccessScheduler myUIAccessScheduler;

  @Nonnull
  protected abstract SingleUIAccessScheduler createScheduler();

  @Nonnull
  @Override
  public UIAccessScheduler getScheduler() {
    if (myUIAccessScheduler == null) {
      myUIAccessScheduler = createScheduler();
    }

    return Objects.requireNonNull(myUIAccessScheduler);
  }
}
