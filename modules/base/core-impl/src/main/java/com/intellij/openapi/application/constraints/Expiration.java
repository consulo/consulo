// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import javax.annotation.Nonnull;

/**
 * Capable of invoking a handler whenever something expires -
 * either a Disposable (see [DisposableExpiration]), or another job (see [JobExpiration]).
 * <p>
 * from kotlin
 */
public interface Expiration {
  interface Handle {
    void unregisterHandler();
  }

  /**
   * Tells whether the handle has expired *and* every expiration handler has finished.
   * Returns false when checked from inside an expiration handler.
   */
  boolean isExpired();

  /**
   * The caller must ensure the returned handle is properly disposed.
   */
  @Nonnull
  Handle invokeOnExpiration(Runnable runnable);
}
