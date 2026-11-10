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
package consulo.ui.ex.action;

import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
import consulo.util.collection.ContainerUtil;


/**
 * Default implementation of the {@link ShortcutSet} interface.
 */
public final class CustomShortcutSet implements ShortcutSet {

  public static final CustomShortcutSet EMPTY = new CustomShortcutSet(Shortcut.EMPTY_ARRAY);

  private final Shortcut[] myShortcuts;

  /**
   * Creates <code>CustomShortcutSet</code> which contains only one
   * single stroke keyboard shortcut.
   */
  public CustomShortcutSet(KeyStroke... keyStrokes){
    this(ContainerUtil.map(keyStrokes, keyStroke -> new KeyboardShortcut(keyStroke, null), Shortcut.EMPTY_ARRAY));
  }

  /**
   * Creates <code>CustomShortcutSet</code> which contains specified keyboard and
   * mouse shortcuts.
   *
   * @param shortcuts keyboard shortcuts
   */
  public CustomShortcutSet(Shortcut... shortcuts){
    myShortcuts = shortcuts.length == 0 ? Shortcut.EMPTY_ARRAY : shortcuts.clone();
  }

  public CustomShortcutSet(KeyCode... keyCodes) {
    myShortcuts = ContainerUtil.map(keyCodes, keyCode -> new KeyboardShortcut(KeyStroke.pressed(keyCode), null), Shortcut.EMPTY_ARRAY);
  }

  @Override
  
  public Shortcut[] getShortcuts(){
    return myShortcuts.length == 0 ? Shortcut.EMPTY_ARRAY : myShortcuts.clone();
  }

  
  public static CustomShortcutSet fromString(String... keyboardShortcuts) {
    KeyboardShortcut[] shortcuts = new KeyboardShortcut[keyboardShortcuts.length];
    for (int i = 0; i < keyboardShortcuts.length; i++) {
      shortcuts[i] = KeyboardShortcut.fromString(keyboardShortcuts[i]);
    }
    return new CustomShortcutSet(shortcuts);
  }
}
