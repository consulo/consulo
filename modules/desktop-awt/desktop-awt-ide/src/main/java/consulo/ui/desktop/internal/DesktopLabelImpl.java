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

import com.intellij.ui.components.JBLabel;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.image.Image;
import consulo.ui.util.MnemonicInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class DesktopLabelImpl extends SwingComponentDelegate<DesktopLabelImpl.MyJLabel> implements Label {
  public class MyJLabel extends JBLabel implements FromSwingComponentWrapper {
    private LocalizeValue myTextValue;

    private HorizontalAlignment myHorizontalAlignment2 = HorizontalAlignment.LEFT;

    private ColorValue myForegroudColor;

    MyJLabel(@Nonnull LocalizeValue text, LabelOptions options) {
      super("");

      setHorizontalAlignment2(options.getHorizontalAlignment());

      myTextValue = text;

      updateText();
    }

    @Override
    public void updateUI() {
      super.updateUI();

      updateText();
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopLabelImpl.this;
    }

    public void setForegroundColor(ColorValue foregroundColor) {
      myForegroudColor = foregroundColor;
      
      updateForegroundColor();
    }

    private void setHorizontalAlignment2(@Nonnull HorizontalAlignment horizontalAlignment) {
      myHorizontalAlignment2 = horizontalAlignment;
      switch (horizontalAlignment) {
        case LEFT:
          setHorizontalAlignment(SwingConstants.LEFT);
          break;
        case CENTER:
          setHorizontalAlignment(SwingConstants.CENTER);
          break;
        case RIGHT:
          setHorizontalAlignment(SwingConstants.RIGHT);
          break;
      }
    }

    public HorizontalAlignment getHorizontalAlignment2() {
      return myHorizontalAlignment2;
    }

    @Nonnull
    public LocalizeValue getTextValue() {
      return myTextValue;
    }

    public void setTextValue(@Nonnull LocalizeValue textValue) {
      myTextValue = textValue;
    }

    private void updateForegroundColor() {
      if(myForegroudColor == null) {
        setForeground(null);
      }
      else {
        setForeground(TargetAWT.to(myForegroudColor));
      }
    }

    private void updateText() {
      if (myTextValue == null) {
        return;
      }

      String text = myTextValue.getValue();
      MnemonicInfo mnemonicInfo = MnemonicInfo.parse(text);
      if (mnemonicInfo == null) {
        setText(text);
        setDisplayedMnemonicIndex(-1);
        setDisplayedMnemonic(0);
      }
      else {
        setText(mnemonicInfo.getText());
        setDisplayedMnemonicIndex(mnemonicInfo.getIndex());
        setDisplayedMnemonic(mnemonicInfo.getKeyCode());
      }
    }
  }

  public DesktopLabelImpl(LocalizeValue text, LabelOptions options) {
    initialize(new MyJLabel(text, options));
  }

  @Override
  public void setImage(@Nullable Image icon) {
    toAWTComponent().setIcon(TargetAWT.to(icon));
  }

  @Nullable
  @Override
  public Image getImage() {
    return TargetAWT.from(toAWTComponent().getIcon());
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull LocalizeValue text) {
    toAWTComponent().setTextValue(text);
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return toAWTComponent().getTextValue();
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return toAWTComponent().getToolTipText();
  }

  @Override
  @RequiredUIAccess
  public void setToolTipText(@Nullable String text) {
    toAWTComponent().setToolTipText(text);
  }

  @Override
  public void setForegroundColor(ColorValue colorValue) {
    toAWTComponent().setForegroundColor(colorValue);
  }

  @Nullable
  @Override
  public ColorValue getForegroundColor() {
    return toAWTComponent().myForegroudColor;
  }
}
