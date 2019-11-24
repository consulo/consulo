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

import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.image.Image;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.HorizontalAlignment;
import consulo.ui.style.ComponentColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class DesktopLabelImpl extends SwingComponentDelegate<JLabel> implements Label {
  class MyJLabel extends JLabel implements FromSwingComponentWrapper {
    MyJLabel(String text) {
      super(text);
    }

    @Override
    public void updateUI() {
      super.updateUI();

      DesktopLabelImpl.this.updateUI();
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopLabelImpl.this;
    }
  }

  private HorizontalAlignment myHorizontalAlignment = HorizontalAlignment.LEFT;
  private Supplier<ColorValue> myForegroundSupplier;

  public DesktopLabelImpl(String text) {
    myComponent = new MyJLabel(text);

    setHorizontalAlignment(HorizontalAlignment.LEFT);

    myForegroundSupplier = () -> ComponentColors.TEXT;

    updateUI();
  }

  private void updateUI() {
    // not initialized
    if (myComponent == null) {
      return;
    }
    myComponent.setForeground(TargetAWT.to(myForegroundSupplier.get()));
  }

  @Nonnull
  @Override
  public Label setImage(@Nullable Image icon) {
    myComponent.setIcon(TargetAWT.to(icon));
    return this;
  }

  @Nullable
  @Override
  public Image getImage() {
    return TargetAWT.from(myComponent.getIcon());
  }

  @Nonnull
  @Override
  public String getText() {
    return myComponent.getText();
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public Label setText(@Nonnull String text) {
    myComponent.setText(text);
    return this;
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return myComponent.getToolTipText();
  }

  @Nonnull
  @Override
  public Label setToolTipText(@Nullable String text) {
    myComponent.setToolTipText(text);
    return this;
  }

  @Nonnull
  @Override
  public Label setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment) {
    myHorizontalAlignment = horizontalAlignment;
    switch (horizontalAlignment) {
      case LEFT:
        myComponent.setHorizontalAlignment(SwingConstants.LEFT);
        break;
      case CENTER:
        myComponent.setHorizontalAlignment(SwingConstants.CENTER);
        break;
      case RIGHT:
        myComponent.setHorizontalAlignment(SwingConstants.RIGHT);
        break;
    }
    return this;
  }

  @Nonnull
  @Override
  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }

  @Nonnull
  @Override
  public Label setForeground(@Nonnull Supplier<ColorValue> colorValueSupplier) {
    myForegroundSupplier = colorValueSupplier;

    myComponent.setForeground(TargetAWT.to(myForegroundSupplier.get()));
    return this;
  }
}
