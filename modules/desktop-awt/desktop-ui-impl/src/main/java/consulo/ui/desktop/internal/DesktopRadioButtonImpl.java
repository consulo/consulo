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
package consulo.ui.desktop.internal;

import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class DesktopRadioButtonImpl extends SwingComponentDelegate<JRadioButton> implements RadioButton {
  class MyJRadioButton extends JRadioButton implements FromSwingComponentWrapper {
    MyJRadioButton(String text, boolean selected) {
      super(text, selected);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopRadioButtonImpl.this;
    }
  }

  public DesktopRadioButtonImpl(String text, boolean selected) {
    myComponent = new MyJRadioButton(text, selected);
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueListener<Boolean> valueListener) {
    DesktopValueListenerAsItemListenerImpl<Boolean> listener = new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, false);
    myComponent.addItemListener(listener);
    return () -> myComponent.removeItemListener(listener);
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return myComponent.isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nonnull Boolean value, boolean fireListeners) {
    myComponent.setSelected(value);
  }

  @Nonnull
  @Override
  public String getText() {
    return myComponent.getText();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    myComponent.setText(text);
  }
}
