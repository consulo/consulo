// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util;

import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.EditorEx;
import jakarta.annotation.Nonnull;

/**
 * @deprecated Use {@link EditorEx#setContextMenuGroupId(String)} or
 * {@link EditorEx#installPopupHandler(consulo.codeEditor.EditorPopupHandler)} instead. To be removed in version 2020.2.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
//@ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
public abstract class EditorPopupHandler implements EditorMouseListener {
  public abstract void invokePopup(EditorMouseEvent event);

  private void handle(EditorMouseEvent e) {
    if (e.getMouseEvent().isPopupTrigger() && e.getArea() == EditorMouseEventArea.EDITING_AREA) {
      invokePopup(e);
      e.consume();
    }
  }

  @Override
  public void mouseClicked(@Nonnull EditorMouseEvent e) {
    handle(e);
  }

  @Override
  public void mousePressed(@Nonnull EditorMouseEvent e) {
    handle(e);
  }

  @Override
  public void mouseReleased(@Nonnull EditorMouseEvent e) {
    handle(e);
  }
}