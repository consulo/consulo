/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ui.components;

import com.intellij.util.ui.UIUtil;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author evgeny.zakrevsky
 */
public class JBLabelDecorator extends JBLabel {
  private JBLabelDecorator() {
    super();
  }

  private JBLabelDecorator(@Nullable Image image) {
    super(image);
  }

  private JBLabelDecorator(@Nonnull String text) {
    super(text);
  }

  private JBLabelDecorator(@Nonnull String text, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(text, horizontalAlignment);
  }

  private JBLabelDecorator(@Nullable Image image, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(image, horizontalAlignment);
  }

  private JBLabelDecorator(@Nonnull String text, @Nullable Image icon, @JdkConstants.HorizontalAlignment int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
  }

  public static JBLabelDecorator createJBLabelDecorator() {
    return new JBLabelDecorator();
  }

  public static JBLabelDecorator createJBLabelDecorator(String text) {
    return new JBLabelDecorator(text);
  }

  public JBLabelDecorator setBold(boolean isBold) {
    if (isBold) {
      setFont(getFont().deriveFont(Font.BOLD));
    } else {
      setFont(getFont().deriveFont(Font.PLAIN));
    }
    return this;
  }

  public JBLabelDecorator setComponentStyleDecorative(@Nonnull UIUtil.ComponentStyle componentStyle) {
    super.setComponentStyle(componentStyle);
    return this;
  }

  public JBLabelDecorator setFontColorDecorative(@Nonnull UIUtil.FontColor fontColor) {
    super.setFontColor(fontColor);
    return this;
  }
}
