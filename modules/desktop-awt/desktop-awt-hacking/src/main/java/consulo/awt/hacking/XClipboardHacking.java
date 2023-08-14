/*
 * Copyright 2013-2023 consulo.io
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
package consulo.awt.hacking;

import consulo.util.lang.lazy.LazyValue;

import java.awt.datatransfer.Clipboard;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 14/08/2023
 */
public class XClipboardHacking {
  private static final String ourClassName = "sun.awt.X11.XClipboard";

  public static Supplier<Method> getClipboardFormatsMethod = LazyValue.nullable(() -> {
    try {
      Class<?> clipboardClass = Class.forName(ourClassName);
      Method method = clipboardClass.getDeclaredMethod("getClipboardFormats");
      method.setAccessible(true);
      return method;
    }
    catch (Throwable ignored) {
    }
    return null;
  });

  public static boolean isAvailable() {
    return getClipboardFormatsMethod.get() != null;
  }

  public static long[] getClipboardFormats(Clipboard clipboard) {
    if (!ourClassName.equals(clipboard.getClass().getName()) || !isAvailable()) {
      return null;
    }

    Method method = getClipboardFormatsMethod.get();
    try {
      return (long[])method.invoke(clipboard);
    }
    catch (Throwable ignored) {
    }
    return null;
  }
}
