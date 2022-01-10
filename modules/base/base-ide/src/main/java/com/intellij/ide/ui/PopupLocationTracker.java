// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * This tracker helps to avoid visual overlapping when several 'popups' has to be shown simultaneously
 * 'Popups' here are abstract and may have different nature like Balloons (virtual popups in owner's LayeredPane) and AbstractPopups (real heavyweight windows)
 * Non-modal dialogs also could be tracked if need
 * <p>
 * Example of overlapping: completion in editor together with Javadoc on mouse over (or with inspection hint)
 */
//@ApiStatus.Experimental
public class PopupLocationTracker {

  private static final Collection<ScreenAreaConsumer> ourAreaConsumers = new LinkedHashSet<>();

  public static boolean register(@Nonnull ScreenAreaConsumer consumer) {
    if (!Registry.is("ide.use.screen.area.tracker", false)) {
      return true;
    }
    if (!Disposer.isDisposed(consumer) && ourAreaConsumers.add(consumer)) {
      Disposer.register(consumer, new Disposable() {
        @Override
        public void dispose() {
          ourAreaConsumers.remove(consumer);
        }
      });
      return true;
    }
    return false;
  }

  public static boolean canRectangleBeUsed(@Nonnull Component parent, @Nonnull Rectangle desiredScreenBounds, @Nullable ScreenAreaConsumer excludedConsumer) {
    if (!Registry.is("ide.use.screen.area.tracker", false)) {
      return true;
    }
    Window window = UIUtil.getWindow(parent);
    if (window != null) {
      for (ScreenAreaConsumer consumer : ourAreaConsumers) {
        if (consumer == excludedConsumer) continue;

        if (window == consumer.getUnderlyingWindow()) {
          Rectangle area = consumer.getConsumedScreenBounds();
          if (area.intersects(desiredScreenBounds)) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
