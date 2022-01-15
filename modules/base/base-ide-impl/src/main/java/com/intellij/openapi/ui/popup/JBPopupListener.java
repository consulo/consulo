// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.ui.popup;

import javax.annotation.Nonnull;

public interface JBPopupListener {
  default void beforeShown(@Nonnull LightweightWindowEvent event) {
  }

  default void onClosed(@Nonnull LightweightWindowEvent event) {
  }

  /**
   * @deprecated Use {@link JBPopupListener} directly.
   */
  @Deprecated
  class Adapter implements JBPopupListener {
  }
}
