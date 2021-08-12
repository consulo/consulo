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
import consulo.ui.Button;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ClickListener;
import consulo.ui.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtButtonImpl extends SWTComponentDelegate<org.eclipse.swt.widgets.Button> implements Button {
  private LocalizeValue myText;

  public DesktopSwtButtonImpl(LocalizeValue text) {
    myText = text;
  }

  @Override
  protected org.eclipse.swt.widgets.Button createSWT(Composite parent) {
    return new org.eclipse.swt.widgets.Button(parent, SWT.NONE);
  }

  @Override
  protected void initialize(org.eclipse.swt.widgets.Button component) {
    component.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        getListenerDispatcher(ClickListener.class).clicked(new ClickEvent(DesktopSwtButtonImpl.this));
      }
    });
    component.setText(myText.get());
  }

  @Nonnull
  @Override
  public String getText() {
    return myText.toString();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    myText = LocalizeValue.of(text);
  }

  @Nullable
  @Override
  public Image getIcon() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setIcon(@Nullable Image image) {

  }
}
