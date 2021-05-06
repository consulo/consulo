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

import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.IntSlider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 11/29/2020
 */
public class DesktopIntSliderImpl extends SwingComponentDelegate<DesktopIntSliderImpl.MyJSlider> implements IntSlider {
  public class MyJSlider extends JSlider implements FromSwingComponentWrapper {
    public MyJSlider(int min, int max, int value) {
      super(min, max, value);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopIntSliderImpl.this;
    }
  }

  public DesktopIntSliderImpl(int min, int max, int value) {
    MyJSlider slider = new MyJSlider(min, max, value);
    initialize(slider);
    slider.addChangeListener(e -> fireListeners());
  }

  @Override
  public void setRange(int min, int max) {
    toAWTComponent().setMinimum(min);
    toAWTComponent().setMaximum(max);
  }

  @Nullable
  @Override
  public Integer getValue() {
    return toAWTComponent().getValue();
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {
    toAWTComponent().setValue(value);

    if (fireListeners) {
      fireListeners();
    }
  }

  @RequiredUIAccess
  private void fireListeners() {
    getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(this, getValue()));
  }
}
