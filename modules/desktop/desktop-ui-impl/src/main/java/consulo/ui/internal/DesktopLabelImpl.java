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
package consulo.ui.internal;

import consulo.awt.TargetAWT;
import consulo.ui.Label;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.HorizontalAlignment;
import consulo.ui.style.ComponentColors;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopLabelImpl extends SwingComponentDelegate<JLabel> implements Label, SwingWrapper {
  private HorizontalAlignment myHorizontalAlignment = HorizontalAlignment.LEFT;
  private Supplier<ColorValue> myForegroundSupplier;

  public DesktopLabelImpl(String text) {
    myComponent = new JLabel(text) {
      @Override
      public void updateUI() {
        super.updateUI();

        DesktopLabelImpl.this.updateUI();
      }
    };

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

  @NotNull
  @Override
  public String getText() {
    return myComponent.getText();
  }

  @RequiredUIAccess
  @Override
  public void setText(@NotNull String text) {
    myComponent.setText(text);
  }

  @Override
  public void setHorizontalAlignment(@NotNull HorizontalAlignment horizontalAlignment) {
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
  }

  @NotNull
  @Override
  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }

  @Override
  public void setForeground(@NotNull Supplier<ColorValue> colorValueSupplier) {
    myForegroundSupplier = colorValueSupplier;
  }
}
