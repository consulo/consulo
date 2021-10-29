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

import com.intellij.ui.components.JBRadioButton;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class DesktopRadioButtonImpl extends SwingComponentDelegate<DesktopRadioButtonImpl.MyJBRadioButton> implements RadioButton {
  class MyJBRadioButton extends JBRadioButton implements FromSwingComponentWrapper {
    private LocalizeValue myLabelText = LocalizeValue.empty();

    @Override
    public void updateUI() {
      super.updateUI();

      // null if called from parent object before field initialize
      if (myLabelText != null) {
        updateLabelText();
      }
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopRadioButtonImpl.this;
    }

    public void setLabelText(LocalizeValue labelText) {
      myLabelText = labelText;
    }

    public LocalizeValue getLabelText() {
      return myLabelText;
    }

    private void updateLabelText() {
      updateTextForButton(this, myLabelText);
    }
  }

  public DesktopRadioButtonImpl(LocalizeValue textValue, boolean selected) {
    MyJBRadioButton component = new MyJBRadioButton();
    component.setSelected(selected);
    initialize(component);
    setLabelText(textValue);
    component.addActionListener(e -> fireListeners());
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return toAWTComponent().isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nonnull Boolean value, boolean fireListeners) {
    toAWTComponent().setSelected(value);

    if (fireListeners) {
      fireListeners();
    }
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  private void fireListeners() {
    getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(this, toAWTComponent().isSelected()));
  }

  @Nonnull
  @Override
  public LocalizeValue getLabelText() {
    return toAWTComponent().getLabelText();
  }

  @RequiredUIAccess
  @Override
  public void setLabelText(@Nonnull LocalizeValue labelText) {
    toAWTComponent().setLabelText(labelText);
    toAWTComponent().updateLabelText();
  }
}
