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

import javax.annotation.Nonnull;
import javax.swing.*;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Jun-22
 */
public abstract class ConfigurableScrollBarUI extends DefaultScrollBarUI {
  protected enum Style {
    Legacy,
    Overlay
  }

  public ConfigurableScrollBarUI() {
  }

  public ConfigurableScrollBarUI(int thickness, int thicknessMax, int thicknessMin) {
    super(thickness, thicknessMax, thicknessMin);
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    updateStyle(getCurrentStyle());
    processReferences(this, null, null);
  }

  @Override
  public final void uninstallUI(JComponent c) {
    processReferences(null, this, null);
    cancelAllRequests();
    super.uninstallUI(c);
  }

  protected abstract void processReferences(ConfigurableScrollBarUI toAdd, ConfigurableScrollBarUI toRemove, List<? super ConfigurableScrollBarUI> list);

  protected void cancelAllRequests() {
  }

  @Nonnull
  public abstract Style getCurrentStyle();

  protected void updateStyle(Style style) {
    if (myScrollBar != null) {
      myScrollBar.setOpaque(style != Style.Overlay);
      myScrollBar.revalidate();
      myScrollBar.repaint();
      onThumbMove();
    }
  }
}
