// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.ui;

import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import java.awt.*;

public interface ScreenAreaConsumer extends Disposable {
  @Nonnull
  Rectangle getConsumedScreenBounds();

  Window getUnderlyingWindow();
}
