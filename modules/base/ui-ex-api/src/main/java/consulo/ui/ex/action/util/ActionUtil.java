/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.action.util;

import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author VISTALL
 * @since 09-Mar-22
 */
public class ActionUtil {
  public static void registerForEveryKeyboardShortcut(@Nonnull JComponent component, @Nonnull ActionListener action, @Nonnull ShortcutSet shortcuts) {
    for (Shortcut shortcut : shortcuts.getShortcuts()) {
      if (shortcut instanceof KeyboardShortcut) {
        KeyboardShortcut ks = (KeyboardShortcut)shortcut;
        KeyStroke first = ks.getFirstKeyStroke();
        KeyStroke second = ks.getSecondKeyStroke();
        if (second == null) {
          component.registerKeyboardAction(action, first, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
      }
    }
  }
}
