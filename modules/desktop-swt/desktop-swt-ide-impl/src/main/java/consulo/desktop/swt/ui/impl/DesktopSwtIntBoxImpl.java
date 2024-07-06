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
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtIntBoxImpl extends SWTComponentDelegate<Spinner> implements IntBox {
  private int myInitValue;
  private int myInitMin;
  private int myInitMax = Integer.MAX_VALUE;

  public DesktopSwtIntBoxImpl(int initValue) {
    myInitValue = initValue;
  }

  @Override
  public void setPlaceholder(@Nullable String text) {

  }

  @Override
  public void setRange(int min, int max) {
    Spinner spinner = toSWTComponent();
    if (spinner != null) {
      spinner.setMinimum(min);
      spinner.setMaximum(max);
    }
    else {
      myInitMin = min;
      myInitMax = max;
    }
  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Override
  public void setFocusable(boolean focusable) {

  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Nonnull
  @Override
  public Disposable addValidator(@Nonnull Validator<Integer> validator) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public boolean validate() {
    return true;
  }

  @Nullable
  @Override
  public Integer getValue() {
    Spinner spinner = toSWTComponent();
    return spinner.getSelection();
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {
    Spinner spinner = toSWTComponent();
    if (spinner != null) {
      spinner.setSelection(value == null ? 0 : value);
    }
    else {
      myInitValue = value;
    }
  }

  @Override
  protected Spinner createSWT(Composite parent) {
    return new Spinner(parent, SWT.BORDER);
  }

  @Override
  protected void initialize(Spinner component) {
    super.initialize(component);

    component.setSelection(myInitValue);
    component.setMinimum(myInitMin);
    component.setMaximum(myInitMax);
  }
}
