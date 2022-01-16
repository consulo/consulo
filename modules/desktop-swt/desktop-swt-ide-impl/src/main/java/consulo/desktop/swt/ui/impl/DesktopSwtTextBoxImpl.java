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

import consulo.disposer.Disposable;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtTextBoxImpl extends SWTComponentDelegate<Text> implements TextBox {
  private String myText;

  public DesktopSwtTextBoxImpl(String text) {
    myText = StringUtil.notNullize(text);
  }

  @Override
  protected Text createSWT(Composite parent) {
    return new Text(parent, SWT.BORDER);
  }

  @Override
  protected void initialize(Text component) {
    super.initialize(component);
    component.setText(myText);
  }

  @Override
  public void selectAll() {

  }

  @Override
  public void setEditable(boolean editable) {

  }

  @Override
  public boolean isEditable() {
    return false;
  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Nonnull
  @Override
  public Disposable addValidator(@Nonnull Validator<String> validator) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public boolean validate() {
    return false;
  }

  @Nullable
  @Override
  public String getValue() {
    return myText;
  }

  @RequiredUIAccess
  @Override
  public void setValue(String value, boolean fireListeners) {
    myText = StringUtil.notNullize(value);

    if(myComponent != null) {
      myComponent.setText(myText);
    }
  }
}
