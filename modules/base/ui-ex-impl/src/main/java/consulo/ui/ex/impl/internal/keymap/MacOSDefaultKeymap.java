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
package consulo.ui.ex.impl.internal.keymap;

import consulo.ui.event.details.KeyStroke;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.MouseShortcut;
import consulo.ui.ex.action.Shortcut;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author max
 */
public class MacOSDefaultKeymap extends DefaultKeymapImpl {
  @Override
  protected String[] getParentActionIds(KeyStroke firstKeyStroke) {
    return super.getParentActionIds(convertKeyStroke(firstKeyStroke));
  }

  @Override
  protected String[] getParentActionIds(MouseShortcut shortcut) {
    return super.getParentActionIds(convertMouseShortcut(shortcut));
  }

  @Override
  protected Shortcut[] getParentShortcuts(String actionId) {
    Shortcut[] parentShortcuts = super.getParentShortcuts(actionId);
    Shortcut[] macShortcuts = new Shortcut[parentShortcuts.length];
    for (int i = 0; i < parentShortcuts.length; i++) {
      macShortcuts[i] = convertShortcutFromParent(parentShortcuts[i]);
    }
    return macShortcuts;
  }

  public static Shortcut convertShortcutFromParent(Shortcut parentShortcut) {
    if (parentShortcut instanceof MouseShortcut mouseShortcut) {
      return convertMouseShortcut(mouseShortcut);
    }

    KeyboardShortcut key = (KeyboardShortcut)parentShortcut;
    return new KeyboardShortcut(convertKeyStroke(key.getFirstKeyStroke()),
                                convertKeyStroke(key.getSecondKeyStroke()));
  }

  private static @Nullable KeyStroke convertKeyStroke(@Nullable KeyStroke parentKeyStroke) {
    if (parentKeyStroke == null) return null;
    Set<Modifier> modifiers = mapModifiers(parentKeyStroke.getModifiers());
    return parentKeyStroke.isTyped()
      ? KeyStroke.typed(parentKeyStroke.getKeyChar(), modifiers)
      : KeyStroke.of(parentKeyStroke.getKeyCode(), modifiers, parentKeyStroke.isOnKeyRelease());
  }

  private static MouseShortcut convertMouseShortcut(MouseShortcut macShortcut) {
    return new MouseShortcut(macShortcut.getButton(),
                             mapModifiers(macShortcut.getModifiers()),
                             macShortcut.getClickCount());
  }

  /**
   * The keys under Ctrl on other systems are under Command on a Mac, and the other way round.
   */
  private static Set<Modifier> mapModifiers(Set<Modifier> modifiers) {
    Set<Modifier> mapped = EnumSet.noneOf(Modifier.class);
    for (Modifier modifier : modifiers) {
      mapped.add(switch (modifier) {
        case META -> Modifier.CTRL;
        case CTRL -> Modifier.META;
        default -> modifier;
      });
    }
    return mapped;
  }
}
