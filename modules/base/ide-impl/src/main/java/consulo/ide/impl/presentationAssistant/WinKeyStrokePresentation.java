/*
 * Copyright 2013-2017 consulo.io
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

package consulo.ide.impl.presentationAssistant;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 21-Aug-17
 * <p>
 * original author Nikolay Chashnikov (kotlin)
 */
class WinKeyStrokePresentation {
  private static final List<Pair<String, Integer>> inputEventMaskFieldNames;
  private static final Map<Integer, String> keyEventFieldNames;

  static {
    inputEventMaskFieldNames = new ArrayList<>();

    Field[] fields = InputEvent.class.getFields();
    for (Field field : fields) {
      String name = field.getName();
      if (name.endsWith("_MASK") && !name.endsWith("_DOWN_MASK") && !name.startsWith("BUTTON") && Modifier.isStatic(field.getModifiers())) {
        try {
          Object value = field.get(null);

          if (value instanceof Integer) {
            inputEventMaskFieldNames.add(Pair.create(fieldNameToPresentableName(StringUtil.trimEnd(name, "_MASK")), (Integer)value));
          }
        }
        catch (IllegalAccessException e) {
          throw new Error(e);
        }
      }
    }

    keyEventFieldNames = new HashMap<>();
    for (Field field : KeyEvent.class.getFields()) {
      String name = field.getName();
      if (name.startsWith("VK_") && Modifier.isStatic(field.getModifiers())) {
        try {
          Object value = field.get(null);
          if (value instanceof Integer) {
            keyEventFieldNames.put((Integer)value, fieldNameToPresentableName(StringUtil.trimStart(name, "VK_")));
          }
        }
        catch (IllegalAccessException e) {
          throw new Error(e);
        }
      }
    }
  }

  public static String getWinKeyText(int key) {
    switch (key) {
      case KeyEvent.VK_BACK_SPACE:
        return "Backspace";
      case KeyEvent.VK_MULTIPLY:
        return "NumPad *";
      case KeyEvent.VK_ADD:
        return "NumPad +";
      case KeyEvent.VK_SEPARATOR:
        return "NumPad ,";
      case KeyEvent.VK_SUBTRACT:
        return "NumPad -";
      case KeyEvent.VK_DECIMAL:
        return "NumPad .";
      case KeyEvent.VK_DIVIDE:
        return "NumPad /";
      default:
        if (keyEventFieldNames.containsKey(key)) {
          return keyEventFieldNames.get(key);
        }
        return "Unknown key 0x" + Integer.toHexString(key);
    }
  }

  @Nonnull
  public static String getWinModifiersText(int modifiers) {
    String[] array = inputEventMaskFieldNames.stream()
                                             .filter(it -> BitUtil.isSet(modifiers, it.getSecond()))
                                             .map(it -> it.getFirst())
                                             .toArray(String[]::new);

    return String.join("+", array);
  }

  private static String fieldNameToPresentableName(String name) {
    return String.join(" ", ContainerUtil.map(StringUtil.split(name, "_"), s -> StringUtil.capitalize(name.toLowerCase())));
  }
}
