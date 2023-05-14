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
package consulo.application.internal;

import consulo.platform.Platform;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
public class InternalKeyboardLayoutUtil {
  public static final Map<Character, Character> ourLLtoASCII = new ConcurrentHashMap<>();

  @Nullable
  public static Character getAsciiForChar(char a) {
    char lc = Character.toLowerCase(a);
    Character c = ourLLtoASCII.get(lc);
    if (c == null && (ourLLtoASCII.isEmpty() || Platform.current().os().isLinux())) {
      // Linux note:
      // KeyEvent provides 'rawCode' (a physical |row|column| coordinate) instead of 'keyCode'.
      // ASCII rawCodes can be collected to map chars via their rawCode in future.
      // That would also allow map latin chars to latin chars when a layout switches latin keys.
      c = HardCoded.LL.get(lc);
    }
    return c == null ? null : lc == a ? c : Character.toUpperCase(c);
  }

  private static class HardCoded {
    private static final Map<Character, Character> LL = new HashMap<>(33);

    static {
      // keyboard layouts in lowercase
      char[] layout = new char[]{
              // Russian-PC
              'й', 'q', 'ц', 'w', 'у', 'e', 'к', 'r', 'е', 't', 'н', 'y', 'г', 'u', 'ш', 'i', 'щ', 'o', 'з', 'p', 'х', '[', 'ъ', ']', 'ф', 'a', 'ы', 's', 'в', 'd', 'а', 'f', 'п', 'g', 'р', 'h', 'о',
              'j', 'л', 'k', 'д', 'l', 'ж', ';', 'э', '\'', 'я', 'z', 'ч', 'x', 'с', 'c', 'м', 'v', 'и', 'b', 'т', 'n', 'ь', 'm', 'б', ',', 'ю', '.', '.', '/'};
      int i = 0;
      while (i < layout.length) {
        LL.put(layout[i++], layout[i++]);
      }
    }
  }
}
