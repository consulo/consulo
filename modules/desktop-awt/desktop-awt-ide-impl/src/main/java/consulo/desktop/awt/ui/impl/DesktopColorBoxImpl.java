/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.uiOld.ColorPanel;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.ui.ColorBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 6/9/18
 */
class DesktopColorBoxImpl extends SwingComponentDelegate<DesktopColorBoxImpl.MyColorPanel> implements ColorBox {
  class MyColorPanel extends ColorPanel implements FromSwingComponentWrapper {
    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopColorBoxImpl.this;
    }
  }

  public DesktopColorBoxImpl(ColorValue colorValue) {
    MyColorPanel component = new MyColorPanel();
    initialize(component);
    component.setSelectedColor(TargetAWT.to(colorValue));
    component.addActionListener(e -> fireListeners());
  }

  @Nullable
  @Override
  public ColorValue getValue() {
    return TargetAWT.from(toAWTComponent().getSelectedColor());
  }

  @RequiredUIAccess
  @Override
  public void setValue(ColorValue value, boolean fireListeners) {
    toAWTComponent().setSelectedColor(TargetAWT.to(value));

    if(fireListeners) {
      fireListeners();
    }
  }

  @Override
  public void setEditable(boolean editable) {
    toAWTComponent().setEditable(editable);
  }

  @Override
  public boolean isEditable() {
    return toAWTComponent().isEditable();
  }

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  private void fireListeners() {
    getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
  }
}
