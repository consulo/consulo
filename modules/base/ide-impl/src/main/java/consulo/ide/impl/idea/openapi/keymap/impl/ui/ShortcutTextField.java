/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.ui.KeyStrokeAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author Anna.Kozlova
 * @since 2006-08-31
 */
public class ShortcutTextField extends JTextField {
  private KeyStroke myKeyStroke;

  public ShortcutTextField() {
    enableEvents(AWTEvent.KEY_EVENT_MASK);
    setFocusTraversalKeysEnabled(false);
  }

  @Override
  protected void processKeyEvent(KeyEvent e) {
    if (e.getID() == KeyEvent.KEY_PRESSED) {
      int keyCode = e.getKeyCode();
      if (
              keyCode == KeyEvent.VK_SHIFT ||
              keyCode == KeyEvent.VK_ALT ||
              keyCode == KeyEvent.VK_CONTROL ||
              keyCode == KeyEvent.VK_ALT_GRAPH ||
              keyCode == KeyEvent.VK_META
              ){
        return;
      }
      setKeyStroke(KeyStrokeAdapter.getDefaultKeyStroke(e));
    }
  }

  public void setKeyStroke(KeyStroke keyStroke) {
    myKeyStroke = keyStroke;
    setText(KeyboardShortcutDialog.getTextByKeyStroke(keyStroke));
    updateCurrentKeyStrokeInfo();
  }

  protected void updateCurrentKeyStrokeInfo() {
  }

  public KeyStroke getKeyStroke() {
    return myKeyStroke;
  }

  @Override
  public void enableInputMethods(boolean enable) {
    super.enableInputMethods(enable && Registry.is("ide.settings.keymap.input.method.enabled"));
  }
}