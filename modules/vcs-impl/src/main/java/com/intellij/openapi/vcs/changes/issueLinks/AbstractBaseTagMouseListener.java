/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.issueLinks;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.ClickListener;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public abstract class AbstractBaseTagMouseListener extends ClickListener implements MouseMotionListener {
  @Override
  public boolean onClick(MouseEvent e, int clickCount) {
    if (e.getButton() == 1 && !e.isPopupTrigger()) {
      Object tag = getTagAt(e);
      if (tag instanceof Runnable) {
        ((Runnable) tag).run();
        return true;
      }

      if ((tag != null) && (! Object.class.getName().equals(tag.getClass().getName()))) {
        BrowserUtil.launchBrowser(tag.toString());
        return true;
      }
    }
    return false;
  }

  @Nullable
  protected abstract Object getTagAt(final MouseEvent e);
  public void mouseDragged(MouseEvent e) {
  }

  public void mouseMoved(MouseEvent e) {
    Component table = (Component) e.getSource();
    Object tag = getTagAt(e);
    if (tag != null) {
      table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    else {
      table.setCursor(Cursor.getDefaultCursor());
    }
  }

  @Override
  public void installOn(Component c) {
    super.installOn(c);
    c.addMouseMotionListener(this);
  }
}
