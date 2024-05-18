// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.service;

import consulo.ui.ex.awt.LinkMouseListenerBase;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Link mouse listener which stores a tag pointed by a cursor into installed component,
 * changes cursor, and repaint the component if active tag is changed.
 * Tag could be retrieved from the component by {@link RepaintLinkMouseListenerBase#ACTIVE_TAG} key.
 */
public abstract class RepaintLinkMouseListenerBase<T> extends LinkMouseListenerBase<T> {
  public static final Key<Object> ACTIVE_TAG = Key.create("RepaintLinkMouseListenerActiveTag");

  @Override
  public void mouseMoved(MouseEvent e) {
    if (!isEnabled()) return;

    JComponent component = (JComponent)e.getSource();
    Object tag = getTagAt(e);
    UIUtil.setCursor(component, tag != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    if (!Comparing.equal(tag, ComponentUtil.getClientProperty(component, ACTIVE_TAG))) {
      ComponentUtil.putClientProperty(component, ACTIVE_TAG, tag);
      repaintComponent(e);
    }
  }

  @Override
  public void installOn(@Nonnull Component component) {
    if (!(component instanceof JComponent)) {
      throw new IllegalArgumentException("JComponent expected");
    }
    super.installOn(component);

    component.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        JComponent component = (JComponent)e.getSource();
        Object tag = ComponentUtil.getClientProperty(component, ACTIVE_TAG);
        if (tag != null) {
          ComponentUtil.putClientProperty(component, ACTIVE_TAG, null);
          repaintComponent(e);
        }
      }
    });
  }

  protected abstract void repaintComponent(MouseEvent e);

  /**
   * Override this method if link listener should be temporary disabled for a component.
   * For example, if tree is empty and empty text link listener should manage the cursor.
   */
  protected boolean isEnabled() {
    return true;
  }
}
