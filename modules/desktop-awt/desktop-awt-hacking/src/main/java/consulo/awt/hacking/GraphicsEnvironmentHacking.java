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

import sun.awt.DisplayChangedListener;
import sun.java2d.SunGraphicsEnvironment;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public class GraphicsEnvironmentHacking {
  /**
   * @return null if not avaliable
   */
  @Nullable
  public static Boolean isUIScaleEnabled(GraphicsEnvironment ge) throws Exception {
    if (ge instanceof SunGraphicsEnvironment) {
      Method m = isUIScaleEnabled();
      if (m == null) {
        return null;
      }

      if (Modifier.isStatic(m.getModifiers())) {
        return (Boolean)m.invoke(null);
      }
      else {
        return (Boolean)m.invoke(ge);
      }
    }

    return null;
  }

  public static void addDisplayChangeListener(GraphicsEnvironment env, Runnable runnable) throws Throwable {
    if (env instanceof SunGraphicsEnvironment) {
      ((SunGraphicsEnvironment)env).addDisplayChangedListener(new DisplayChangedListener() {
        @Override
        public void displayChanged() {
          runnable.run();
        }

        @Override
        public void paletteChanged() {
          runnable.run();
        }
      });
    }
  }

  @Nullable
  private static Method isUIScaleEnabled() {
    try {
      Method isUIScaleEnabled = SunGraphicsEnvironment.class.getDeclaredMethod("isUIScaleEnabled");
      isUIScaleEnabled.setAccessible(true);
      return isUIScaleEnabled;
    }
    catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }
}
