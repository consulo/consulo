/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.ui.laf.modern;

import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 02.08.14
 */
public class ModernUIUtil {
  private static NotNullLazyValue<Field> ourUiField = new NotNullLazyValue<Field>() {
    @NotNull
    @Override
    protected Field compute() {
      try {
        Field ui = JComponent.class.getDeclaredField("ui");
        ui.setAccessible(true);
        return ui;
      }
      catch (NoSuchFieldException e) {
        throw new Error(e);
      }
    }
  };

  @NotNull
  public static <T> T getUI(Component component) {
    Field value = ourUiField.getValue();
    try {
      return (T)value.get(component);
    }
    catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }

  @NotNull
  public static Color getSelectionBackground() {
    return UIManager.getColor("Color.SelectionBackground");
  }

  @NotNull
  public static Color getBorderColor(@NotNull Component component) {
    return component.isEnabled() ? getActiveBorderColor() : getDisabledBorderColor();
  }

  @NotNull
  public static Color getActiveBorderColor() {
    return UIManager.getColor("Color.ActiveBorder");
  }

  @NotNull
  public static Color getDisabledBorderColor() {
    return UIManager.getColor("Color.DisabledBorder");
  }
}
