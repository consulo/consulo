/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.ui.components.OnOffButton;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.ToggleSwitch;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-26
 */
public class DesktopToggleSwitchImpl extends SwingComponentDelegate<DesktopToggleSwitchImpl.MyOnOffButton> implements ToggleSwitch {
  class MyOnOffButton extends OnOffButton implements FromSwingComponentWrapper {
    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopToggleSwitchImpl.this;
    }
  }

  public DesktopToggleSwitchImpl(boolean enabled) {
    MyOnOffButton component = new MyOnOffButton();
    initialize(component);
    component.setSelected(enabled);
    component.addActionListener(e -> fireListeners());
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return toAWTComponent().isSelected();
  }

  @RequiredUIAccess
  @Override
  @SuppressWarnings("unchecked")
  public void setValue(@Nonnull Boolean value, boolean fireListeners) {
    toAWTComponent().setSelected(value);

    if(fireListeners) {
      fireListeners();
    }
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  private void fireListeners() {
    getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(this, toAWTComponent().isSelected()));
  }
}
