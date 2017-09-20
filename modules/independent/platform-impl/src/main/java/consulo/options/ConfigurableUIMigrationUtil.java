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
package consulo.options;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 05-Nov-16
 */
public class ConfigurableUIMigrationUtil {

  @RequiredDispatchThread
  public static JComponent createComponent(@NotNull UnnamedConfigurable configurable) {
    consulo.ui.Component uiComponent = configurable.createUIComponent();
    if (uiComponent != null) {
      return (JComponent)uiComponent;
    }
    return configurable.createComponent();
  }

  @RequiredDispatchThread
  public static JComponent getPreferredFocusedComponent(@NotNull Configurable.HoldPreferredFocusedComponent component) {
    consulo.ui.Component uiComponent = component.getPreferredFocusedUIComponent();
    if (uiComponent != null) {
      return (JComponent)uiComponent;
    }
    return component.getPreferredFocusedComponent();
  }
}
