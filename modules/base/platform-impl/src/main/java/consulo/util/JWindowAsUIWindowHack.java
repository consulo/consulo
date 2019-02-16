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
package consulo.util;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ReflectionUtil;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author VISTALL
 * @since 2019-02-16
 * <p>
 * hack due desktop module hold JWindowAsUIWindow, not base plaform
 */
@Deprecated
public class JWindowAsUIWindowHack {
  private static NotNullLazyValue<Constructor<?>> jWindowAsUIWindowContructor = NotNullLazyValue.createValue(() -> {
    Class windowClass = ReflectionUtil.forName("consulo.ui.desktop.internal.window.JWindowAsUIWindow");
    try {
      return windowClass.getConstructor(Window.class);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  });

  @Nonnull
  public static JWindow create(Window window) {
    try {
      return (JWindow)jWindowAsUIWindowContructor.getValue().newInstance(window);
    }
    catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
