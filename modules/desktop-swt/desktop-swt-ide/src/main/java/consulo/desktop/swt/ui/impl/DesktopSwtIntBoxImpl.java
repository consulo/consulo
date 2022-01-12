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
import org.eclipse.swt.widgets.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtIntBoxImpl extends SWTComponentDelegate<Text> implements IntBox {
  private final int myValue;

  public DesktopSwtIntBoxImpl(int value) {
    myValue = value;
  }

  @Override
  public void setPlaceholder(@Nullable String text) {

  }

  @Override
  public void setRange(int min, int max) {

  }

  @Override
  public boolean hasFocus() {
    return false;
  }

  @Nonnull
  @Override
  public Disposable addValidator(@Nonnull Validator<Integer> validator) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public boolean validate() {
    return false;
  }

  @Nullable
  @Override
  public Integer getValue() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setValue(Integer value, boolean fireListeners) {

  }

  @Override
  protected Text createSWT(Composite parent) {
    return new Text(parent, SWT.BORDER);
  }
}
