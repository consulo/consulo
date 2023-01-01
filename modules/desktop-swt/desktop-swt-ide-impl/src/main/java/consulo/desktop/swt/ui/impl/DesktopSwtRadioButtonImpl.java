/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtRadioButtonImpl extends SWTComponentDelegate<Button> implements RadioButton {
  private LocalizeValue myText;
  private boolean mySelected;

  public DesktopSwtRadioButtonImpl(LocalizeValue text, boolean selected) {
    myText = text;
    mySelected = selected;
  }

  @Override
  protected void initialize(Button component) {
    super.initialize(component);

    component.setText(myText.get());
    component.setSelection(mySelected);
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return mySelected;
  }

  @RequiredUIAccess
  @Override
  public void setValue(@Nonnull Boolean value, boolean fireListeners) {
    mySelected = value;

    if(myComponent != null) {
      myComponent.setSelection(value);
    }
  }

  @Nonnull
  @Override
  public LocalizeValue getLabelText() {
    return myText;
  }

  @RequiredUIAccess
  @Override
  public void setLabelText(@Nonnull LocalizeValue text) {
    myText = text;

    if(myComponent != null) {
      myComponent.setText(text.get());
    }
  }

  @Override
  protected Button createSWT(Composite parent) {
    return new Button(parent, SWT.RADIO);
  }
}
