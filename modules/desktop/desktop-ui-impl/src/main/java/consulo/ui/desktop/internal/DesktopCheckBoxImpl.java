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

import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBCheckBox;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.KeyCode;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.util.MnemonicInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
class DesktopCheckBoxImpl extends SwingComponentDelegate<DesktopCheckBoxImpl.MyJBCheckBox> implements CheckBox {
  class MyJBCheckBox extends JBCheckBox implements FromSwingComponentWrapper {
    private LocalizeValue myLabelText = LocalizeValue.empty();

    @Override
    public void updateUI() {
      super.updateUI();

      // null if called from parent object before field initialize
      if(myLabelText != null) {
        updateLabelText();
      }
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopCheckBoxImpl.this;
    }

    public void setLabelText(LocalizeValue labelText) {
      myLabelText = labelText;
    }

    public LocalizeValue getLabelText() {
      return myLabelText;
    }

    private void updateLabelText() {
      String text = myLabelText.getValue();

      MnemonicInfo mnemonicInfo = MnemonicInfo.parse(text);
      if (mnemonicInfo == null) {
        toAWTComponent().setText(text);

        setMnemonic(0);
        setDisplayedMnemonicIndex(-1);
      }
      else {
        toAWTComponent().setText(mnemonicInfo.getText());
        setMnemonic(TargetAWT.to(mnemonicInfo.getKeyCode()));
        setDisplayedMnemonicIndex(mnemonicInfo.getIndex());
      }
    }
  }

  public DesktopCheckBoxImpl() {
    initialize(new MyJBCheckBox());
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return toAWTComponent().isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nullable Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

    toAWTComponent().setSelected(value);
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

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    ItemListener listener = e -> {
      if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
        valueListener.valueChanged(new ValueEvent<>(this, toAWTComponent().isSelected()));
      }
    };
    toAWTComponent().addItemListener(listener);
    return () -> toAWTComponent().removeItemListener(listener);
  }

  @Override
  public void setMnemonicKey(@Nullable KeyCode key) {
    toAWTComponent().setMnemonic(key == null ? 0 : TargetAWT.to(key));
  }

  @Override
  public void setMnemonicTextIndex(int index) {
    toAWTComponent().setDisplayedMnemonicIndex(index);
  }
}
