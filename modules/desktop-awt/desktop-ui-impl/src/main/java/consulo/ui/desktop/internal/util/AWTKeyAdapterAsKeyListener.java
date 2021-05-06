/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.util;

import com.intellij.util.BitUtil;
import consulo.ui.Component;
import consulo.ui.event.KeyListener;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2019-11-07
 */
public class AWTKeyAdapterAsKeyListener extends KeyAdapter {
  private final Component myComponent;
  private final KeyListener myKeyListener;

  public AWTKeyAdapterAsKeyListener(Component component, KeyListener keyListener) {
    myComponent = component;
    myKeyListener = keyListener;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    myKeyListener.keyPressed(new consulo.ui.event.KeyEvent(myComponent, e.getKeyCode(), getUIModifiers(e)));
  }

  @Override
  public void keyReleased(KeyEvent e) {
    myKeyListener.keyPressed(new consulo.ui.event.KeyEvent(myComponent, e.getKeyCode(), getUIModifiers(e)));
  }

  private static int getUIModifiers(KeyEvent e) {
    int value = 0;

    value = BitUtil.set(value, consulo.ui.event.KeyEvent.M_ALT, e.isAltDown());
    value = BitUtil.set(value, consulo.ui.event.KeyEvent.M_CTRL, e.isControlDown());
    value = BitUtil.set(value, consulo.ui.event.KeyEvent.M_SHIFT, e.isShiftDown());
    return value;
  }
}
