// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.awt.hacking;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SequencedEventNestedFieldHolder {
  public static final Field NESTED_FIELD;
  public static final Method DISPOSE_METHOD;
  public static final Class<?> SEQUENCED_EVENT_CLASS;

  public static void invokeDispose(AWTEvent event) {
    try {
      DISPOSE_METHOD.invoke(event);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  static {
    try {
      SEQUENCED_EVENT_CLASS = Class.forName("java.awt.SequencedEvent");
      NESTED_FIELD = SEQUENCED_EVENT_CLASS.getDeclaredField("nested");
      NESTED_FIELD.setAccessible(true);

      DISPOSE_METHOD = SEQUENCED_EVENT_CLASS.getDeclaredMethod("dispose");
      DISPOSE_METHOD.setAccessible(true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
