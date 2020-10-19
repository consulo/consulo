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

import consulo.awt.hacking.util.FieldAccessor;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public class JComponentHacking {
  private static final FieldAccessor<JComponent, Object> clientProperties = new FieldAccessor<>(JComponent.class, "clientProperties");

  private static Method ourSafelyGetGraphicsMethod;

  static {
    try {
      ourSafelyGetGraphicsMethod = JComponent.class.getDeclaredMethod("safelyGetGraphics", Component.class);
      ourSafelyGetGraphicsMethod.setAccessible(true);
    }
    catch (NoSuchMethodException ignored) {
    }
  }

  @Nullable
  public static Graphics safelyGetGraphics(Component component) {
    if (ourSafelyGetGraphicsMethod == null) {
      return null;
    }
    try {
      return (Graphics)ourSafelyGetGraphicsMethod.invoke(null, component);
    }
    catch (IllegalAccessException | InvocationTargetException ignored) {
      return null;
    }
  }

  public static void setClientProperties(JComponent component, Object value) {
    if(clientProperties.isAvailable()) {
      clientProperties.set(component, value);
    }
  }
}
