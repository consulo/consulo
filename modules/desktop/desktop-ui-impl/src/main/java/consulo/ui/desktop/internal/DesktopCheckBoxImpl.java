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
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.util.MnemonicInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.ActionListener;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
class DesktopCheckBoxImpl extends SwingComponentDelegate<JBCheckBox> implements CheckBox {
  class MyJBCheckBox extends JBCheckBox implements FromSwingComponentWrapper {

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopCheckBoxImpl.this;
    }
  }

  public DesktopCheckBoxImpl() {
    myComponent = new MyJBCheckBox();
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return myComponent.isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nullable Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

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
    MnemonicInfo mnemonicInfo = MnemonicInfo.parse(text);
    if (mnemonicInfo == null) {
      myComponent.setText(text);

      setMnemonicKey(null);
      setMnemonicTextIndex(-1);
    }
    else {
      myComponent.setText(mnemonicInfo.getText());
      setMnemonicKey(mnemonicInfo.getKeyCode());
      setMnemonicTextIndex(mnemonicInfo.getIndex());
    }
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    ActionListener listener = e -> valueListener.valueChanged(new ValueEvent<>(this, myComponent.isSelected()));
    myComponent.addActionListener(listener);
    return () -> myComponent.removeActionListener(listener);
  }

  @Override
  public void setMnemonicKey(@Nullable KeyCode key) {
    myComponent.setMnemonic(key == null ? 0 : TargetAWT.to(key));
  }

  @Override
  public void setMnemonicTextIndex(int index) {
    myComponent.setDisplayedMnemonicIndex(index);
  }
}
