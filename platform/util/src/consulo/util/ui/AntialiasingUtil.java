/*
 * Copyright 2013-2016 consulo.io
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
package consulo.util.ui;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.PairConsumer;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.swing.SwingUtilities2;

import java.awt.*;
import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 22-Nov-16.
 */
public class AntialiasingUtil {
  private static boolean ourJava9 = SystemInfo.isJavaVersionAtLeast("1.9");

  private static final NotNullLazyValue<Object> ourAAFieldValue = new NotNullLazyValue<Object>() {
    @NotNull
    @Override
    protected Object compute() {
      return ReflectionUtil.getField(SwingUtilities2.class, null, Object.class, "AA_TEXT_PROPERTY_KEY");
    }
  };

  private static final NotNullLazyValue<Constructor<?>> ourAATextInfoConstructor = new NotNullLazyValue<Constructor<?>>() {
    @NotNull
    @Override
    protected Constructor<?> compute() {
      try {
        Class<?> clazz = Class.forName("sun.swing.SwingUtilities2$AATextInfo");
        return clazz.getConstructor(Object.class, Integer.class);
      }
      catch (Exception e) {
        throw new Error(e);
      }
    }
  };

  public static void setup(@NotNull PairConsumer<Object, Object> component, @Nullable AATextInfo value) {
    if(ourJava9) {
      component.consume(RenderingHints.KEY_TEXT_ANTIALIASING, value == null ? null : value.getRenderHint());
      component.consume(RenderingHints.KEY_TEXT_LCD_CONTRAST, value == null ? null : value.getLcdValue());
    }
    else {
      try {
        Object insertValue = value == null ? null : ourAATextInfoConstructor.getValue().newInstance(value.getRenderHint(), value.getLcdValue());
        component.consume(ourAAFieldValue.getValue(), insertValue);

      }
      catch (Exception e) {
        throw new Error(e);
      }
    }
  }
}
