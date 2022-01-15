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
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 05-Nov-16
 */
public class ConfigurableUIMigrationUtil {

  @RequiredUIAccess
  public static JComponent createComponent(@Nonnull UnnamedConfigurable configurable, @Nonnull Disposable parentUIDisposable) {
    JComponent component = configurable.createComponent(parentUIDisposable);
    if (component != null) {
      return component;
    }

    consulo.ui.Component uiComponent = configurable.createUIComponent(parentUIDisposable);
    if (uiComponent != null) {
      return (JComponent)TargetAWT.to(uiComponent);
    }
    return null;
  }

  @RequiredUIAccess
  public static JComponent getPreferredFocusedComponent(@Nonnull Configurable.HoldPreferredFocusedComponent component) {
    JComponent preferredFocusedComponent = component.getPreferredFocusedComponent();
    if(preferredFocusedComponent != null) {
      return preferredFocusedComponent;
    }
    consulo.ui.Component uiComponent = component.getPreferredFocusedUIComponent();
    if (uiComponent != null) {
      return (JComponent)TargetAWT.to(uiComponent);
    }
    return null;
  }
}
