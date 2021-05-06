/*
 * Copyright 2013-2019 consulo.io
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

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2019-11-20
 */
public class AWTKeyStrokeHacking {
  private static Method getCachedStrokeMethod;
  static {

    try {
      getCachedStrokeMethod = AWTKeyStroke.class.getDeclaredMethod("getCachedStroke", char.class, int.class, int.class, boolean.class);
      getCachedStrokeMethod.setAccessible(true);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static AWTKeyStroke getCachedStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
    try {
      return (AWTKeyStroke)getCachedStrokeMethod.invoke(null, keyChar, keyCode, modifiers, onKeyRelease);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
