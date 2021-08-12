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
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtLabelImpl extends SWTComponentDelegate<org.eclipse.swt.widgets.Label> implements Label {
  private LocalizeValue myText = LocalizeValue.empty();

  public DesktopSwtLabelImpl(LocalizeValue text) {
    myText = text;
  }

  @Override
  protected org.eclipse.swt.widgets.Label createSWT(Composite parent) {
    return new org.eclipse.swt.widgets.Label(parent, SWT.NONE);
  }

  @Override
  protected void initialize(org.eclipse.swt.widgets.Label component) {
    component.setText(myText.get());
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return myText;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull LocalizeValue text) {
    myText = text;
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return null;
  }

  @Override
  public void setToolTipText(@Nullable String text) {

  }

  @Override
  public void setImage(@Nullable Image icon) {

  }

  @Nullable
  @Override
  public Image getImage() {
    return null;
  }
}
