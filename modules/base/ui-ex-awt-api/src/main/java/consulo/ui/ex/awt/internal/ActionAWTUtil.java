/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui.ex.awt.internal;

import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.keymap.KeymapManager;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2025-08-21
 */
public class ActionAWTUtil {
    public static InputEvent getInputEvent(String actionName) {
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionName);
        KeyStroke keyStroke = null;
        for (Shortcut each : shortcuts) {
            if (each instanceof KeyboardShortcut) {
                keyStroke = ((KeyboardShortcut) each).getFirstKeyStroke();
                break;
            }
        }

        if (keyStroke != null) {
            return new KeyEvent(JOptionPane.getRootFrame(),
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                keyStroke.getModifiers(),
                keyStroke.getKeyCode(),
                keyStroke.getKeyChar(),
                KeyEvent.KEY_LOCATION_STANDARD);
        }
        else {
            return new MouseEvent(JOptionPane.getRootFrame(), MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1);
        }
    }
}
