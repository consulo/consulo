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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.wm.WindowInfo;
import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowStripeButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtToolWindowStripeButtonImpl extends SWTComponentDelegate<Button> implements ToolWindowStripeButton {
  private final DesktopSwtToolWindowInternalDecorator myInternalDecorator;

  public DesktopSwtToolWindowStripeButtonImpl(DesktopSwtToolWindowInternalDecorator internalDecorator, DesktopSwtToolWindowPanelImpl toolWindowPanel) {
    myInternalDecorator = internalDecorator;
  }

  @Override
  protected Button createSWT(Composite parent) {
    return new Button(parent, SWT.DEFAULT);
  }

  @Override
  protected void initialize(Button component) {
    super.initialize(component);

    component.setText(getWindowInfo().getId());
  }

  @Nonnull
  @Override
  public WindowInfo getWindowInfo() {
    return myInternalDecorator.getWindowInfo();
  }

  @Override
  public void apply(@Nonnull WindowInfo windowInfo) {

  }

  @RequiredUIAccess
  @Override
  public void updatePresentation() {

  }

  @Nonnull
  @Override
  public Component getComponent() {
    return this;
  }
}
