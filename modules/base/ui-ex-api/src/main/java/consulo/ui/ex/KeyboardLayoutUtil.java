/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex;

import consulo.application.internal.InternalKeyboardLayoutUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gregsh
 */
public class KeyboardLayoutUtil extends InternalKeyboardLayoutUtil {
  @Nullable
  public static Character getAsciiForChar(char a) {
    return InternalKeyboardLayoutUtil.getAsciiForChar(a);
  }

  public static void storeAsciiForChar(@Nonnull KeyEvent e) {
    int id = e.getID();
    if (id != KeyEvent.KEY_PRESSED) return;
    int mods = e.getModifiers();
    int code = e.getKeyCode();
    char aChar = e.getKeyChar();
    if ((mods & ~InputEvent.SHIFT_MASK & ~InputEvent.SHIFT_DOWN_MASK) != 0) return;

    if (code < KeyEvent.VK_A || code > KeyEvent.VK_Z) return;
    if (aChar == KeyEvent.CHAR_UNDEFINED) return;
    if ('a' <= aChar && aChar <= 'z' || 'A' <= aChar && aChar <= 'Z') return;
    if (ourLLtoASCII.containsKey(aChar)) return;
    ourLLtoASCII.put(aChar, (char)((int)'a' + (code - KeyEvent.VK_A)));
  }
}
