/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.ui.laf.modern;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 02.08.14
 */
public class MouseEnterHandler extends MouseAdapter {
  private boolean myMouseInside;

  public MouseEnterHandler(final JComponent component) {
    component.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        myMouseInside = true;
        component.repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myMouseInside = false;
        component.repaint();
      }
    });
  }

  public boolean isMouseInside() {
    return myMouseInside;
  }
}
