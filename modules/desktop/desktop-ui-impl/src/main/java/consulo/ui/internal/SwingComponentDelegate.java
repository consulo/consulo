/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal;

import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.impl.SomeUIWrapper;
import consulo.ui.impl.UIDataObject;
import consulo.ui.migration.ToSwingWrapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public class SwingComponentDelegate<T extends JComponent> implements Component, ToSwingWrapper, SomeUIWrapper {
  protected T myComponent;

  @Nonnull
  @Override
  public java.awt.Component toAWT() {
    return myComponent;
  }

  @Override
  public boolean isVisible() {
    return myComponent.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myComponent.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myComponent.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myComponent.setEnabled(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)myComponent.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    myComponent.setPreferredSize(TargetAWT.to(size));
  }

  @Nonnull
  @Override
  public UIDataObject dataObject() {
    javax.swing.JComponent component = (javax.swing.JComponent)toAWT();
    UIDataObject dataObject = (UIDataObject)component.getClientProperty(UIDataObject.class);
    if (dataObject == null) {
      component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
    }
    return dataObject;
  }
}
