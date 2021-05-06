/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.ui.laf.modern;

import com.intellij.util.BitUtil;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 02.08.14
 */
class MouseEnterHandler extends MouseAdapter {
  private static final int ENTERED = 1 << 0;
  private static final int PRESSED = 1 << 1;

  private int myFlags;
  private MouseAdapter myMouseAdapter;

  public MouseEnterHandler(final Component component) {
    myMouseAdapter = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        if (!component.isEnabled()) {
          return;
        }
        myFlags = BitUtil.set(myFlags, ENTERED, true);
        component.repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myFlags = BitUtil.set(myFlags, ENTERED, false);
        component.repaint();
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (!component.isEnabled()) {
          return;
        }
        myFlags = BitUtil.set(myFlags, PRESSED, true);
        component.repaint();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        myFlags = BitUtil.set(myFlags, PRESSED, false);
        component.repaint();
      }
    };
  }

  public void replace(Component oldComponent, Component newComponent) {
    if (oldComponent != null) {
      oldComponent.removeMouseListener(myMouseAdapter);
    }

    if (newComponent != null) {
      newComponent.addMouseListener(myMouseAdapter);
    }
  }

  public boolean isMousePressed() {
    return BitUtil.isSet(myFlags, PRESSED);
  }

  public boolean isMouseEntered() {
    return BitUtil.isSet(myFlags, ENTERED);
  }
}
