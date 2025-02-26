/*
 * Copyright 2013-2024 consulo.io
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

import consulo.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 12-Jul-24
 */
public class WindowHacking {
  private static final Logger LOG = Logger.getInstance(WindowHacking.class);

  private static Method isModalBlockedMethod = null;
  private static Method getModalBlockerMethod = null;

  static {
    Class[] noParams = new Class[]{};

    try {
      isModalBlockedMethod = Window.class.getDeclaredMethod("isModalBlocked", noParams);
      getModalBlockerMethod = Window.class.getDeclaredMethod("getModalBlocker", noParams);
      isModalBlockedMethod.setAccessible(true);
      getModalBlockerMethod.setAccessible(true);
    }
    catch (NoSuchMethodException e) {
      LOG.error(e);
    }


  }

  public static boolean isModalBlocked(final Window window) {
    boolean result = false;
    try {
      result = (Boolean)isModalBlockedMethod.invoke(window);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return result;
  }

  public static JDialog getModalBlockerFor(final Window window) {
    JDialog result = null;
    try {
      result = (JDialog)getModalBlockerMethod.invoke(window);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return result;
  }
}
