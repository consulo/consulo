// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.util;

import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;

import jakarta.annotation.Nonnull;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import static consulo.application.util.registry.Registry.intValue;

/**
 * This helper class is intended to prevent opening a popup right after its closing.
 *
 * @see consulo.ide.impl.idea.ui.popup.PopupState
 * @deprecated use another PopupState instead
 */
@Deprecated
public class PopupState implements JBPopupListener, PopupMenuListener {
  private boolean hidden = true;
  private long time;

  private void markAsShown() {
    hidden = false;
  }

  private void markAsHidden() {
    hidden = true;
    time = System.currentTimeMillis();
  }

  public boolean isRecentlyHidden() {
    if (!hidden) return false;
    hidden = false;
    return (System.currentTimeMillis() - time) < intValue("ide.popup.hide.show.threshold", 200);
  }

  // JBPopupListener

  @Override
  public void beforeShown(@Nonnull LightweightWindowEvent event) {
    markAsShown();
  }

  @Override
  public void onClosed(@Nonnull LightweightWindowEvent event) {
    markAsHidden();
  }

  // PopupMenuListener

  @Override
  public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
    markAsShown();
  }

  @Override
  public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
    markAsHidden();
  }

  @Override
  public void popupMenuCanceled(PopupMenuEvent event) {
  }
}
