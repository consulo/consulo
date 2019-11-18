/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.util.awt.laf;

import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.SystemInfo;
import consulo.annotations.ReviewAfterMigrationToJRE;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 09-Sep-17
 */
@ReviewAfterMigrationToJRE(9)
public class PreJava9UIUtil {
  private static NotNullLazyValue<Constructor> ourAATextInfoConstructor = NotNullLazyValue.createValue(new NotNullFactory<Constructor>() {
    @Nonnull
    @Override
    public Constructor create() {
      try {
        Class<?> clazz = Class.forName("sun.swing.SwingUtilities2$AATextInfo");
        return clazz.getDeclaredConstructors()[0];
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }
  });

  private static NotNullLazyValue<Object> ourAATextPropertyKey = NotNullLazyValue.createValue(new NotNullFactory<Object>() {
    @Nonnull
    @Override
    public Object create() {
      try {
        Class<?> aClass = Class.forName("sun.swing.SwingUtilities2");
        Field field = aClass.getDeclaredField("AA_TEXT_PROPERTY_KEY");
        return field.get(null);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  });

  @Nonnull
  public static Object AA_TEXT_PROPERTY_KEY() {
    if (SystemInfo.IS_AT_LEAST_JAVA9) {
      throw new IllegalArgumentException("java 9 restricted");
    }

    return ourAATextPropertyKey.getValue();
  }

  @Nonnull
  public static Object newAATextInfo(Object hint, Integer value) {
    if (SystemInfo.IS_AT_LEAST_JAVA9) {
      throw new IllegalArgumentException("java 9 restricted");
    }

    try {
      return ourAATextInfoConstructor.getValue().newInstance(hint, value);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
