/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ui.ex.awt.ScrollingUtil;
import consulo.util.concurrent.ActionCallback;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * A simple way to delegate standard actions of a JList such as move up, down, home, end, page up, page down
 * to another component. Example, a focused textfield re-sends these actions to unfocused popup with a JList inside
 * <p>
 * Use returned ActionCallback object to unsubscribe the listener
 *
 * @author Konstantin Bulenkov
 */
public class ListDelegationUtil {
  public static ActionCallback installKeyboardDelegation(final JComponent focusedComponent, final JList delegateTo) {
    final KeyListener keyListener = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        final int code = e.getKeyCode();
        final int modifiers = e.getModifiers();
        switch (code) {
          case KeyEvent.VK_UP:
            ScrollingUtil.moveUp(delegateTo, modifiers);
            break;
          case KeyEvent.VK_DOWN:
            ScrollingUtil.moveDown(delegateTo, modifiers);
            break;
          case KeyEvent.VK_HOME:
            ScrollingUtil.moveHome(delegateTo);
            break;
          case KeyEvent.VK_END:
            ScrollingUtil.moveEnd(delegateTo);
            break;
          case KeyEvent.VK_PAGE_UP:
            ScrollingUtil.movePageUp(delegateTo);
            break;
          case KeyEvent.VK_PAGE_DOWN:
            ScrollingUtil.movePageDown(delegateTo);
            break;
        }
      }
    };

    focusedComponent.addKeyListener(keyListener);

    final ActionCallback callback = new ActionCallback();
    callback.doWhenProcessed(() -> focusedComponent.removeKeyListener(keyListener));
    return callback;
  }
}
