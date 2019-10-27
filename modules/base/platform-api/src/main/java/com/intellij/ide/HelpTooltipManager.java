// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * @author Alexander Lobas
 */
public class HelpTooltipManager extends HelpTooltip {
  public HelpTooltipManager() {
    getDismissDelay();
    createMouseListeners();
  }

  public void showTooltip(@Nonnull JComponent component, @Nonnull MouseEvent event) {
    initPopupBuilder(new HelpTooltip().setTitle(component.getToolTipText(event)));

    if (event.getID() == MouseEvent.MOUSE_ENTERED) {
      myMouseListener.mouseEntered(event);
    }
    else {
      myMouseListener.mouseMoved(event);
    }
  }

  public void hideTooltip() {
    hidePopup(true);
  }
}