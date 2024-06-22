/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.impl.internal.pom;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.util.lang.function.ThrowableRunnable;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19-Jun-24
 */
public class PomAspectGuard {
  private static volatile boolean allowPsiModification = true;

  @RequiredWriteAction
  public static <T extends Throwable> void guardPsiModificationsIn(@Nonnull Application application,
                                                                   @Nonnull ThrowableRunnable<T> runnable) throws T {
    application.assertWriteAccessAllowed();
    boolean old = allowPsiModification;
    try {
      allowPsiModification = false;
      runnable.run();
    }
    finally {
      allowPsiModification = old;
    }
  }

  public static boolean isAllowPsiModification() {
    return allowPsiModification;
  }
}
