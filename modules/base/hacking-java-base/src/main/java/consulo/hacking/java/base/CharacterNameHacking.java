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
package consulo.hacking.java.base;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * @author Bas Leijdekkers
 */
public class CharacterNameHacking {
  @SuppressWarnings("unchecked")
  private static Method getMethod(Class clazz, String name, Class... params) {
    try {
      Method method = clazz.getDeclaredMethod(name, params);
      method.setAccessible(true);
      return method;
    }
    catch (NoSuchMethodException ignored) {
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static Field getField(Class clazz, String name) {
    try {
      Field declaredField = clazz.getDeclaredField(name);
      declaredField.setAccessible(true);
      return declaredField;
    }
    catch (NoSuchFieldException ignored) {
    }
    return null;
  }

  public static void iterate(Consumer<String> consumer) {
    try {
      final Class<?> aClass = Class.forName("java.lang.CharacterName");
      final Method instance = getMethod(aClass, "getInstance");
      final Field field1 = getField(aClass, "strPool");
      final Field field2 = getField(aClass, "lookup");
      if (instance != null && field1 != null && field2 != null) { // jdk 9
        final Object characterName = instance.invoke(null);
        byte[] namePool = (byte[])field1.get(characterName);
        final int[] lookup = (int[])field2.get(characterName);
        for (int index : lookup) {
          if (index != 0) {
            final String name = new String(namePool, index >>> 8, index & 0xff, StandardCharsets.US_ASCII);
            consumer.accept(name);
          }
        }
      }
    }
    catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getCodePoint(String name) {
    if (name == null) {
      return -1;
    }
    final Method method = getMethod(Character.class, "codePointOf", String.class); // jdk 9 method
    if (method != null) {
      try {
        return (int)method.invoke(null, name);
      }
      catch (IllegalArgumentException e) {
        return -1;
      }
      catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
    return getUnnamedUnicodeCharacterCodePoint(name);
  }

  private static int getUnnamedUnicodeCharacterCodePoint(String name) {
    int index = name.lastIndexOf(' ');
    if (index != -1) {
      try {
        int c = Integer.parseInt(name.substring(index + 1, name.length()), 16);
        if (Character.isValidCodePoint(c) && name.equals(Character.getName(c))) {
          return c;
        }
      }
      catch (NumberFormatException ignore) {
      }
    }
    return -1;
  }
}
