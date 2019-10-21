// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * NB. Methods defined in this interface must be used with great care, this is purely to expose internals required for implementing
 * scheduling methods and coroutine dispatching support.
 *
 * @author eldar
 *
 * from kotlin
 */
public interface ConstrainedExecutionScheduler {
  default void scheduleWithinConstraints(Runnable runnable) {
    scheduleWithinConstraints(runnable, null);
  }

  void scheduleWithinConstraints(Runnable runnable, @Nullable BooleanSupplier condition);
}
