/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal.laf;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 10-Jun-22
 */
public final class WindowsScrollBarUI extends ConfigurableScrollBarUI {
  private static final UIElementWeakStorage<ConfigurableScrollBarUI> UI = new UIElementWeakStorage<>();

  private static final WindowsScrollBarOptionTracker TRACKER = new WindowsScrollBarOptionTracker(UI);
  private static final AtomicBoolean TRACKER_SET = new AtomicBoolean();

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    if (TRACKER_SET.compareAndSet(false, true))  {
      Toolkit.getDefaultToolkit().addAWTEventListener(TRACKER, AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }
  }

  @Override
  boolean isTrackExpandable() {
    return !isOpaque(myScrollBar);
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return TRACKER.isDynamicScrollBars() ? Style.Overlay : Style.Legacy;
  }

  @Override
  protected void processReferences(ConfigurableScrollBarUI toAdd, ConfigurableScrollBarUI toRemove, List<? super ConfigurableScrollBarUI> list) {
    UI.processReferences(toAdd, toRemove, list);
  }
}
