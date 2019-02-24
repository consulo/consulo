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
package consulo.desktop.util.awt;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ReflectionUtil;
import consulo.ui.migration.SwingImageRef;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-24
 */
@Deprecated
public class AllIconsHack {
  // com.intellij.icons.AllIcons
  // AllIcons.Mac.Tree_white_right_arrow

  private static NotNullLazyValue<Icon> Tree_white_right_arrow_value = NotNullLazyValue.createValue(() -> {
    try {
      Class<?> macClazz = Class.forName("com.intellij.icons.AllIcons$Mac");
      return ReflectionUtil.getStaticFieldValue(macClazz, SwingImageRef.class, "Tree_white_right_arrow");
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  });

  private static NotNullLazyValue<Icon> Tree_white_down_arrow_value = NotNullLazyValue.createValue(() -> {
    try {
      Class<?> macClazz = Class.forName("com.intellij.icons.AllIcons$Mac");
      return ReflectionUtil.getStaticFieldValue(macClazz, SwingImageRef.class, "Tree_white_down_arrow");
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  });

  public static Icon Tree_white_right_arrow() {
    return Tree_white_right_arrow_value.getValue();
  }

  public static Icon Tree_white_down_arrow() {
    return Tree_white_down_arrow_value.getValue();
  }
}
