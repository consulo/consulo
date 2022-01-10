// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui;

import consulo.disposer.Disposable;
import javax.annotation.Nonnull;

import java.awt.*;

public interface ScreenAreaConsumer extends Disposable {
  @Nonnull
  Rectangle getConsumedScreenBounds();

  Window getUnderlyingWindow();
}
