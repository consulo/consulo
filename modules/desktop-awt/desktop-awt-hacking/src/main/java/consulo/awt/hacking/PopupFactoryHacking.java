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

import javax.annotation.Nonnull;
import javax.swing.*;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public class PopupFactoryHacking {

  public static void setPopupType(@Nonnull final PopupFactory factory, final int type) {
    try {
      final Method method = PopupFactory.class.getDeclaredMethod("setPopupType", int.class);
      method.setAccessible(true);
      method.invoke(factory, type);
    }
    catch (Throwable ignored) {
    }
  }

  public static int getPopupType(@Nonnull final PopupFactory factory) {
    try {
      final Method method = PopupFactory.class.getDeclaredMethod("getPopupType");
      method.setAccessible(true);
      final Object result = method.invoke(factory);
      return result instanceof Integer ? (Integer)result : -1;
    }
    catch (Throwable ignored) {
    }

    return -1;
  }
}
