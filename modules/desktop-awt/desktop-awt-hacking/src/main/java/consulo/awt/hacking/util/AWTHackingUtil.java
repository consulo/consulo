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
package consulo.awt.hacking.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2019-11-21
 */
public class AWTHackingUtil {
  @Nonnull
  public static Method findMethodWithRuntimeException(Class<?> clazz, String name, Class... params) {
    try {
      Method declaredMethod = clazz.getDeclaredMethod(name, params);
      declaredMethod.setAccessible(true);
      return declaredMethod;
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static Method findMethodSilent(Class<?> clazz, String name, Class... params) {
    try {
      Method declaredMethod = clazz.getDeclaredMethod(name, params);
      declaredMethod.setAccessible(true);
      return declaredMethod;
    }
    catch (Throwable e) {
      if(e instanceof InaccessibleObjectException) {
        throw (InaccessibleObjectException) e;
      }
      return null;
    }
  }
}
